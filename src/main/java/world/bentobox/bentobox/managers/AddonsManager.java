package world.bentobox.bentobox.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.permissions.DefaultPermissions;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Addon.State;
import world.bentobox.bentobox.api.addons.AddonClassLoader;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.addons.Pladdon;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonDescriptionException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonFormatException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonInheritException;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.events.addon.AddonEvent;
import world.bentobox.bentobox.commands.BentoBoxCommand;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento, ComminQ
 */
public class AddonsManager {

    private static final String DEFAULT = ".default";

    private static final String GAMEMODE = "[gamemode].";

    private static final @NotNull String BENTOBOX = "/bentobox";

    @NonNull
    private final List<Addon> addons;
    @NonNull
    private final Map<@NonNull Addon, @Nullable AddonClassLoader> loaders;
    @NonNull
    private final Map<@NonNull Addon, @Nullable Plugin> pladdons;
    @NonNull
    private final Map<String, Class<?>> classes;
    private final BentoBox plugin;
    @NonNull
    private final Map<@NonNull String, @Nullable GameModeAddon> worldNames;
    @NonNull
    private final Map<@NonNull Addon, @NonNull List<Listener>> listeners;

    public AddonsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        addons = new ArrayList<>();
        loaders = new HashMap<>();
        pladdons = new HashMap<>();
        classes = new HashMap<>();
        listeners = new HashMap<>();
        worldNames = new HashMap<>();
    }

    /**
     * Register a plugin as an addon
     * @param parent - parent plugin
     * @param addon - addon class
     */
    public void registerAddon(Plugin parent, Addon addon) {
        plugin.log("Registering " + parent.getDescription().getName());

        // Get description in the addon.yml file
        InputStream resource = parent.getResource("addon.yml");
        if (resource == null) {
            plugin.logError("Failed to register addon: no addon.yml found");
            return;
        }
        // Open a reader to the jar
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            setAddonFile(parent, addon);
            // Grab the description in the addon.yml file
            YamlConfiguration data = new YamlConfiguration();
            data.load(reader);
            // Description
            addon.setDescription(AddonClassLoader.asDescription(data));
            // Set various files
            addon.setDataFolder(parent.getDataFolder());
            // Initialize
            initializeAddon(addon);
            sortAddons();

        } catch (Exception e) {
            plugin.logError("Failed to register addon: " + e);
        }

    }

    private void setAddonFile(Plugin parent, Addon addon) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
        getFileMethod.setAccessible(true);
        addon.setFile((File) getFileMethod.invoke(parent));
    }

    /**
     * Loads all the addons from the addons folder
     */
    public void loadAddons() {
        plugin.log("Loading addons...");
        File f = new File(plugin.getDataFolder(), "addons");
        if (!f.exists() && !f.mkdirs()) {
            plugin.logError("Cannot create addons folder!");
            return;
        }
        Arrays.stream(Objects.requireNonNull(f.listFiles()))
                .filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(this::loadAddon);
        plugin.log("Loaded " + getLoadedAddons().size() + " addons.");

        if (!getLoadedAddons().isEmpty()) {
            sortAddons();
        }
    }

    private record PladdonData(Addon addon, boolean success) {
    }

    private void loadAddon(@NonNull File f) {
        PladdonData result = new PladdonData(null, false);
        try (JarFile jar = new JarFile(f)) {
            // try loading the addon
            // Get description in the addon.yml file
            YamlConfiguration data = addonDescription(jar);
            // Check if the addon is already loaded (duplicate version?)
            String main = data.getString("main");
            if (main != null && this.getAddonByMainClassName(main).isPresent()) {
                getAddonByMainClassName(main).ifPresent(a -> {
                    plugin.logError("Duplicate addon! Addon " + a.getDescription().getName() + " "
                            + a.getDescription().getVersion() + " has already been loaded!");
                    plugin.logError("Remove the duplicate and restart!");
                });
                return;
            }
            // Load the pladdon or addon if it isn't a pladdon
            result = loadPladdon(data, f);
        } catch (Exception e) {
            // We couldn't load the addon, aborting.
            plugin.logError("Could not load addon '" + f.getName() + "'. Error is: " + e.getMessage());
            plugin.logStacktrace(e);
            return;
        }
        // Success
        if (result.success) {
            // Initialize some settings
            result.addon.setDataFolder(new File(f.getParent(), result.addon.getDescription().getName()));
            result.addon.setFile(f);
            // Initialize addon
            initializeAddon(result.addon);
        }
    }

    private PladdonData loadPladdon(YamlConfiguration data, @NonNull File f) throws InvalidAddonInheritException,
            MalformedURLException, InvalidAddonDescriptionException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, InvalidDescriptionException {
        Addon addon;
        try {
            Plugin pladdon = Bukkit.getPluginManager().loadPlugin(f);
            if (pladdon != null && pladdon instanceof Pladdon pl) {
                addon = pl.getAddon();
                addon.setDescription(AddonClassLoader.asDescription(data));
                // Mark pladdon as enabled.
                pl.setEnabled();
                pladdons.put(addon, pladdon);
            } else {
                // Try to load it as an addon
                BentoBox.getInstance()
                        .log("Failed to load " + f.getName() + ", trying to load it as a BentoBox addon");
                // Addon not pladdon
                AddonClassLoader addonClassLoader = new AddonClassLoader(this, data, f,
                        this.getClass().getClassLoader());
                // Get the addon itself
                addon = addonClassLoader.getAddon();
                // Add to the list of loaders
                loaders.put(addon, addonClassLoader);
            }
        } catch (Exception ex) {
            // Addon not pladdon
            AddonClassLoader addonClassLoader = new AddonClassLoader(this, data, f, this.getClass().getClassLoader());
            // Get the addon itself
            addon = addonClassLoader.getAddon();
            // Add to the list of loaders
            loaders.put(addon, addonClassLoader);
        }
        return new PladdonData(addon, true);
    }

    private void initializeAddon(Addon addon) {
        // Locales
        plugin.getLocalesManager().copyLocalesFromAddonJar(addon);
        plugin.getLocalesManager().loadLocalesFromFile(addon.getDescription().getName());

        // Fire the load event
        new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.LOAD).build();

        // Add it to the list of addons
        addons.remove(addon);
        addons.add(addon);
        // Checks if this addon is compatible with the current BentoBox version.
        if (!isAddonCompatibleWithBentoBox(addon)) {
            // It is not, abort.
            plugin.logError("Cannot load " + addon.getDescription().getName() + " because it requires BentoBox version "
                    + addon.getDescription().getApiVersion() + " or greater.");
            plugin.logError("NOTE: Please update BentoBox.");
            addon.setState(State.INCOMPATIBLE);
            return;
        }

        try {
            addon.setState(Addon.State.LOADED);
            // Run the onLoad.
            addon.onLoad();
            // if game mode, get the world name and generate
            if (addon instanceof GameModeAddon gameMode && !addon.getState().equals(State.DISABLED)) {
                if (!gameMode.getWorldSettings().getWorldName().isEmpty()) {
                    worldNames.put(gameMode.getWorldSettings().getWorldName().toLowerCase(Locale.ENGLISH), gameMode);
                }
            }
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(addon, e);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(addon, e);
        }

    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        if (getLoadedAddons().isEmpty())
            return;
        plugin.log("Enabling game mode addons...");
        // Enable GameModes first, then other addons
        getLoadedAddons().stream().filter(a -> !a.getState().equals(State.DISABLED))
                .filter(GameModeAddon.class::isInstance).forEach(this::enableAddon);
        plugin.log("Enabling other addons...");
        getLoadedAddons().stream().filter(a -> !a.getState().equals(State.DISABLED))
                .filter(g -> !(g instanceof GameModeAddon)).forEach(this::enableAddon);
        // Set perms for enabled addons
        this.getEnabledAddons().forEach(this::setPerms);
        plugin.log("Addons successfully enabled.");
    }

    boolean setPerms(Addon addon) {
        ConfigurationSection perms = addon.getDescription().getPermissions();
        if (perms == null)
            return false;
        for (String perm : perms.getKeys(true)) {
            // Only try to register perms for end nodes
            if (perms.contains(perm + DEFAULT) && perms.contains(perm + ".description")) {
                try {
                    registerPermission(perms, perm);
                } catch (InvalidAddonDescriptionException e) {
                    plugin.logError("Addon " + addon.getDescription().getName() + ": " + e.getMessage());
                }
            }
        }
        return true;
    }

    void registerPermission(ConfigurationSection perms, String perm) throws InvalidAddonDescriptionException {
        String name = perms.getString(perm + DEFAULT);
        if (name == null) {
            throw new InvalidAddonDescriptionException("Permission default is invalid in addon.yml: " + perm + DEFAULT);
        }
        PermissionDefault pd = PermissionDefault.getByName(name);
        if (pd == null) {
            throw new InvalidAddonDescriptionException("Permission default is invalid in addon.yml: " + perm + DEFAULT);
        }
        String desc = perms.getString(perm + ".description");
        // Replace placeholders for Game Mode Addon names
        if (perm.contains(GAMEMODE)) {
            getGameModeAddons().stream().map(Addon::getPermissionPrefix)
                    .forEach(p -> DefaultPermissions.registerPermission(perm.replace(GAMEMODE, p), desc, pd));
        } else {
            // Single perm
            DefaultPermissions.registerPermission(perm, desc, pd);
        }
    }

    /**
     * Enables an addon
     * @param addon addon
     */
    private void enableAddon(Addon addon) {
        plugin.log(
                "Enabling " + addon.getDescription().getName() + " (" + addon.getDescription().getVersion() + ")...");
        try {
            // If this is a GameModeAddon create the worlds, register it and load the blueprints
            if (addon instanceof GameModeAddon gameMode) {
                // Create the gameWorlds
                gameMode.createWorlds();
                if (gameMode.isUsesNewChunkGeneration()) {
                    // Create the seed worlds
                    createSeedWorlds(gameMode);
                }
                plugin.getIWM().addGameMode(gameMode);
                // Save and load blueprints
                plugin.getBlueprintsManager().extractDefaultBlueprints(gameMode);
                plugin.getBlueprintsManager().loadBlueprintBundles(gameMode);
            }
            addon.onEnable();
            if (addon.getState().equals(State.DISABLED)) {
                plugin.log(addon.getDescription().getName() + " is disabled.");
                return;
            }
            if (addon instanceof GameModeAddon gameMode) {
                // Set the worlds for the commands
                gameMode.getPlayerCommand().ifPresent(c -> c.setWorld(gameMode.getOverWorld()));
                gameMode.getAdminCommand().ifPresent(c -> c.setWorld(gameMode.getOverWorld()));
            }
            new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build();
            addon.setState(Addon.State.ENABLED);
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(addon, e);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(addon, e);
        }
    }

    /**
     * Create seed worlds, which are used for deletion
     */
    private void createSeedWorlds(GameModeAddon gameMode) {
        if (gameMode.getOverWorld() != null) {
            seedWorld(gameMode, gameMode.getOverWorld());
        }
        if (gameMode.getNetherWorld() != null) {
            seedWorld(gameMode, gameMode.getNetherWorld());
        }
        if (gameMode.getEndWorld() != null) {
            seedWorld(gameMode, gameMode.getEndWorld());
        }
    }

    /**
     * Removes the temporary seed worlds on shutdown
     */
    public void removeSeedWorlds() {
        plugin.log("Removing temporary seed worlds...");
        this.getGameModeAddons().stream().filter(gm -> gm.isUsesNewChunkGeneration()).forEach(gameMode -> {
            plugin.log("Removing " + gameMode.getDescription().getName());
            if (gameMode.getOverWorld() != null) {
                plugin.log("Removing " + gameMode.getOverWorld().getName() + BENTOBOX);
                removeSeedWorld(gameMode.getOverWorld().getName() + BENTOBOX);
            }
            if (gameMode.getNetherWorld() != null) {
                removeSeedWorld(gameMode.getNetherWorld().getName() + BENTOBOX);
            }
            if (gameMode.getEndWorld() != null) {
                removeSeedWorld(gameMode.getEndWorld().getName() + BENTOBOX);
            }
        });
        plugin.log("Removed temporary seed worlds.");
    }

    private void removeSeedWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null || !world.getName().endsWith(BENTOBOX)) {
            return;
        }
        // Teleport any players out of the world, just in case
        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        // Unload the world
        boolean success = Bukkit.unloadWorld(world, false); // false = do not save
        if (!success) {
            plugin.logWarning("Failed to unload seed world: " + world.getName());
        }
        // Delete the world folder and everything in it
        File path = new File(Bukkit.getWorldContainer(), world.getName());
        this.deleteWorldFolder(path);
    }

    /**
     * Recursive delete
     * @param path path to delete
     */
    private void deleteWorldFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorldFolder(file);
                }
            }
            if (!path.delete()) {
                plugin.logWarning("Failed to delete: " + path.getAbsolutePath());
            }
        }
    }

    private void seedWorld(GameModeAddon gameMode, @NonNull World world) {
        // Use the Flat type of world because this is a copy and no vanilla creation is required
        WorldCreator wc = WorldCreator.name(world.getName() + BENTOBOX).type(WorldType.FLAT)
                .environment(world.getEnvironment()).seed(world.getSeed());
        World w = gameMode.getWorldSettings().isUseOwnGenerator() ? wc.createWorld()
                : wc.generator(world.getGenerator()).createWorld();
        w.setDifficulty(Difficulty.PEACEFUL);
        // Register seed world
        plugin.getIWM().addWorld(w, gameMode);
    }

    /**
     * Handles an addon which failed to load due to an incompatibility (missing class, missing method).
     * @param addon instance of the Addon.
     * @param e - linkage exception
     * @since 1.1
     */
    private void handleAddonIncompatibility(@NonNull Addon addon, LinkageError e) {
        // Set the AddonState as "INCOMPATIBLE".
        addon.setState(Addon.State.INCOMPATIBLE);
        plugin.logWarning("Skipping " + addon.getDescription().getName()
                + " as it is incompatible with the current version of BentoBox or of server software...");
        plugin.logWarning("NOTE: The addon is referring to no longer existing classes.");
        plugin.logWarning("NOTE: DO NOT report this as a bug from BentoBox.");
        StringBuilder a = new StringBuilder();
        addon.getDescription().getAuthors().forEach(author -> a.append(author).append(" "));
        plugin.logError("Please report this stack trace to the addon's author(s): " + a);
        plugin.logStacktrace(e);
    }


    private boolean isAddonCompatibleWithBentoBox(@NonNull Addon addon) {
        @SuppressWarnings("deprecation")
        String v = plugin.getDescription().getVersion();
        return isAddonCompatibleWithBentoBox(addon, v);
    }

    /**
     * Checks if the addon does not explicitly rely on API from a more recent BentoBox version.
     * @param addon instance of the Addon.
     * @param pluginVersion plugin version string.
     * @return {@code true} if the addon relies on available BentoBox API, {@code false} otherwise.
     * @since 1.11.0
     */
    boolean isAddonCompatibleWithBentoBox(@NonNull Addon addon, String pluginVersion) {
        String[] apiVersion = addon.getDescription().getApiVersion().split("\\D");
        String[] bentoboxVersion = pluginVersion.split("\\D");

        for (int i = 0; i < apiVersion.length; i++) {
            int bentoboxNumber = 0;
            if (i < bentoboxVersion.length && Util.isInteger(bentoboxVersion[i], false)) {
                bentoboxNumber = Integer.parseInt(bentoboxVersion[i]);
            }
            int apiNumber = Util.isInteger(apiVersion[i], false) ? Integer.parseInt(apiVersion[i]) : -1;

            if (bentoboxNumber > apiNumber) {
                return true; // BentoBox version is greater than the required version -> compatible
            }
            if (bentoboxNumber < apiNumber) {
                return false; // BentoBox is definitely outdated
            }
            // If it is equal, go to the next number
        }

        return true; // Everything is equal, so return true
    }

    /**
     * Handles an addon which failed to load due to an error.
     * @param addon instance of the Addon.
     * @param throwable Throwable that was thrown and which led to the error.
     * @since 1.1
     */
    private void handleAddonError(@NonNull Addon addon, @NonNull Throwable throwable) {
        // Set the AddonState as "ERROR".
        addon.setState(Addon.State.ERROR);
        plugin.logError("Skipping " + addon.getDescription().getName() + " due to an unhandled exception...");
        // Send stacktrace, required for addon development
        plugin.logStacktrace(throwable);
    }

    /**
     * Reloads all the enabled addons
     */
    public void reloadAddons() {
        disableAddons();
        // Reload BentoBox commands
        new BentoBoxCommand();
        loadAddons();
        enableAddons();
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        if (!getEnabledAddons().isEmpty()) {
            plugin.log("Disabling addons...");
            // Disable addons - pladdons are disabled by the server
            getEnabledAddons().stream().filter(addon -> !pladdons.containsKey(addon)).forEach(this::disable);
            plugin.log("Addons successfully disabled.");
        }
        // Unregister all commands
        plugin.getCommandsManager().unregisterCommands();
        // Delete seed worlds
        removeSeedWorlds();
        // Clear all maps
        listeners.clear();
        pladdons.clear();
        addons.clear();
        loaders.clear();
        classes.clear();
    }

    /**
     * Gets the addon by name
     * @param name addon name, not null
     * @return Optional addon object
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends Addon> Optional<T> getAddonByName(@NonNull String name) {
        return addons.stream().filter(a -> a.getDescription().getName().equalsIgnoreCase(name)).map(a -> (T) a)
                .findFirst();
    }

    /**
     * Gets the addon by main class name
     * @param name - main class name
     * @return Optional addon object
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T extends Addon> Optional<T> getAddonByMainClassName(@NonNull String name) {
        return addons.stream().filter(a -> a.getDescription().getMain().equalsIgnoreCase(name)).map(a -> (T) a)
                .findFirst();
    }

    @NonNull
    private YamlConfiguration addonDescription(@NonNull JarFile jar)
            throws InvalidAddonFormatException, IOException, InvalidConfigurationException {
        // Obtain the addon.yml file
        JarEntry entry = jar.getJarEntry("addon.yml");
        if (entry == null) {
            throw new InvalidAddonFormatException("Addon '" + jar.getName() + "' doesn't contains addon.yml file");
        }
        // Open a reader to the jar
        BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
        // Grab the description in the addon.yml file
        YamlConfiguration data = new YamlConfiguration();
        data.load(reader);
        reader.close();
        return data;
    }

    @NonNull
    public List<Addon> getAddons() {
        return addons;
    }

    /**
     * @return List of enabled game mode addons
     * @since 1.1
     */
    @NonNull
    public List<GameModeAddon> getGameModeAddons() {
        return getEnabledAddons().stream().filter(GameModeAddon.class::isInstance).map(GameModeAddon.class::cast)
                .toList();
    }

    /**
     * Gets an unmodifiable list of Addons that are loaded.
     * @return list of loaded Addons.
     * @since 1.1
     */
    @NonNull
    public List<Addon> getLoadedAddons() {
        return addons.stream().filter(addon -> addon.getState().equals(Addon.State.LOADED)).toList();
    }

    /**
     * Gets an unmodifiable list of Addons that are enabled.
     * @return list of enabled Addons.
     * @since 1.1
     */
    @NonNull
    public List<Addon> getEnabledAddons() {
        return addons.stream().filter(addon -> addon.getState().equals(Addon.State.ENABLED)).toList();
    }

    @Nullable
    public AddonClassLoader getLoader(@NonNull final Addon addon) {
        return loaders.get(addon);
    }

    /**
     * Finds a class by name that has been loaded by this loader
     * @param name name of the class, not null
     * @return Class the class or null if not found
     */
    @Nullable
    public Class<?> getClassByName(@NonNull final String name) {
        try {
            return classes.getOrDefault(name, loaders.values().stream().filter(Objects::nonNull)
                    .map(l -> l.findClass(name, false)).filter(Objects::nonNull).findFirst().orElse(null));
        } catch (Exception ignored) {
            // Ignored.
        }
        return null;
    }

    /**
     * Sets a class that this loader should know about
     *
     * @param name name of the class, not null
     * @param clazz the class, not null
     */
    public void setClass(@NonNull final String name, @NonNull final Class<?> clazz) {
        classes.putIfAbsent(name, clazz);
    }

    /**
     * Sorts the addons into loading order taking into account dependencies
     */
    private void sortAddons() {
        // Lists all available addons as names.
        List<String> names = addons.stream().map(a -> a.getDescription().getName()).toList();

        // Check that any dependencies exist
        Iterator<Addon> addonsIterator = addons.iterator();
        while (addonsIterator.hasNext()) {
            Addon a = addonsIterator.next();
            for (String dependency : a.getDescription().getDependencies()) {
                if (!names.contains(dependency)) {
                    plugin.logError(a.getDescription().getName() + " has dependency on " + dependency
                            + " that does not exist. Addon will not load!");
                    addonsIterator.remove();
                    break;
                }
            }
        }

        // Load dependencies or soft dependencies
        Map<String, Addon> sortedAddons = new LinkedHashMap<>();
        // Start with nodes with no dependencies
        addons.stream()
                .filter(a -> a.getDescription().getDependencies().isEmpty()
                        && a.getDescription().getSoftDependencies().isEmpty())
                .forEach(a -> sortedAddons.put(a.getDescription().getName(), a));
        // Fill remaining
        List<Addon> remaining = addons.stream().filter(a -> !sortedAddons.containsKey(a.getDescription().getName()))
                .toList();

        // Run through remaining addons
        remaining.forEach(addon -> {
            // Get the addon's dependencies.
            List<String> dependencies = new ArrayList<>(addon.getDescription().getDependencies());
            dependencies.addAll(addon.getDescription().getSoftDependencies());

            // Remove already sorted addons (dependencies) from the list
            dependencies.removeIf(sortedAddons::containsKey);

            if (dependencies.stream()
                    .noneMatch(dependency -> addon.getDescription().getDependencies().contains(dependency))) {
                sortedAddons.put(addon.getDescription().getName(), addon);
            }
        });

        addons.clear();
        addons.addAll(sortedAddons.values());
    }

    /**
     * Get the world generator if it exists
     * @param worldName - name of world - case insensitive
     * @param id - specific generator id
     * @return ChunkGenerator or null if none found
     * @since 1.2.0
     */
    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        // Clean up world name
        String w = worldName.replace("_nether", "").replace("_the_end", "").replace(BENTOBOX, "")
                .toLowerCase(Locale.ENGLISH);
        if (worldNames.containsKey(w) && worldNames.get(w) != null) {
            return worldNames.get(w).getDefaultWorldGenerator(worldName, id);
        }
        return null;
    }

    /**
     * Register a listener
     * @param addon - the addon registering
     * @param listener - listener
     */
    public void registerListener(@NonNull Addon addon, @NonNull Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, BentoBox.getInstance());
        listeners.computeIfAbsent(addon, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Disables an addon
     * @param addon - addon
     */
    private void disable(@NonNull Addon addon) {
        // Clear listeners
        if (listeners.containsKey(addon)) {
            listeners.get(addon).forEach(HandlerList::unregisterAll);
            listeners.remove(addon);
        }
        // Unregister flags
        plugin.getFlagsManager().unregister(addon);
        // Disable
        if (addon.isEnabled()) {
            plugin.log("Disabling " + addon.getDescription().getName() + "...");
            try {
                addon.onDisable();
            } catch (Exception e) {
                plugin.logError("Error occurred when disabling addon " + addon.getDescription().getName());
                plugin.logError("Report this to the addon's author(s)");
                addon.getDescription().getAuthors().forEach(plugin::logError);
                plugin.logStacktrace(e);
            }
            new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build();
        }
        // Clear loaders
        if (loaders.containsKey(addon)) {
            Set<String> unmodifiableSet = Collections.unmodifiableSet(loaders.get(addon).getClasses());
            for (String className : unmodifiableSet) {
                classes.remove(className);
            }
            addon.setState(State.DISABLED);
            loaders.remove(addon);
        }
        // Disable pladdons
        /*
        if (pladdons.containsKey(addon)) {
            this.pluginLoader.disablePlugin(Objects.requireNonNull(this.pladdons.get(addon)));
            pladdons.remove(addon);
        }
         */
        // Remove it from the addons list
        addons.remove(addon);
    }

    /*
     * Get a unmodifiable list of addon classes that are of type {@link DataObject}
     * but not {@link ConfigObject}. Configs are not transitioned to database.
     * Used in database transition.
     * @return list of DataObjects
     * @since 1.5.0
     */
    @SuppressWarnings("unchecked")
    public Set<Class<? extends DataObject>> getDataObjects() {
        return classes.values().stream().filter(DataObject.class::isAssignableFrom)
                // Do not include config files
                .filter(c -> !ConfigObject.class.isAssignableFrom(c)).map(c -> (Class<? extends DataObject>) c)
                .collect(Collectors.toSet());
    }


    /**
     * Notifies all addons that BentoBox has loaded all addons
     * @since 1.8.0
     */
    public void allLoaded() {
        this.getEnabledAddons().forEach(this::allLoaded);
    }

    /**
     * This method calls Addon#allLoaded in safe manner. If for some reason addon crashes on Addon#allLoaded, then
     * it will disable itself without harming other addons.
     * @param addon Addon that should trigger Addon#allLoaded method.
     */
    private void allLoaded(@NonNull Addon addon) {
        try {
            addon.allLoaded();
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            this.handleAddonIncompatibility(addon, e);
            // Disable addon.
            this.disable(addon);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            this.handleAddonError(addon, e);
            // Disable addon.
            this.disable(addon);
        }
    }
}
