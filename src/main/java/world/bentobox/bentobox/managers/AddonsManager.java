package world.bentobox.bentobox.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonClassLoader;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonFormatException;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.events.addon.AddonEvent;
import world.bentobox.bentobox.database.objects.DataObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author tastybento, ComminQ
 */
public class AddonsManager {

    @NonNull
    private List<Addon> addons;
    @NonNull
    private Map<@NonNull Addon, @Nullable AddonClassLoader> loaders;
    @NonNull
    private final Map<String, Class<?>> classes;
    private BentoBox plugin;
    private @NonNull Map<@NonNull String, @Nullable GameModeAddon> worldNames;
    private @NonNull Map<@NonNull Addon, @NonNull List<Listener>> listeners;

    public AddonsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        addons = new ArrayList<>();
        loaders = new HashMap<>();
        classes = new HashMap<>();
        listeners = new HashMap<>();
        worldNames = new HashMap<>();
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
        Arrays.stream(Objects.requireNonNull(f.listFiles())).filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(this::loadAddon);
        plugin.log("Loaded " + getLoadedAddons().size() + " addons.");

        if (!getLoadedAddons().isEmpty()) {
            sortAddons();
        }
    }

    private void loadAddon(@NonNull File f) {
        Addon addon;
        AddonClassLoader addonClassLoader;
        try (JarFile jar = new JarFile(f)) {
            // try loading the addon
            // Get description in the addon.yml file
            YamlConfiguration data = addonDescription(jar);

            // Load the addon
            addonClassLoader = new AddonClassLoader(this, data, f, this.getClass().getClassLoader());

            // Get the addon itself
            addon = addonClassLoader.getAddon();
        } catch (Exception e) {
            // We couldn't load the addon, aborting.
            plugin.logError(e.getMessage());
            return;
        }

        // Initialize some settings
        addon.setDataFolder(new File(f.getParent(), addon.getDescription().getName()));
        addon.setFile(f);

        // Locales
        plugin.getLocalesManager().copyLocalesFromAddonJar(addon);
        plugin.getLocalesManager().loadLocalesFromFile(addon.getDescription().getName());

        // Fire the load event
        Bukkit.getPluginManager().callEvent(new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.LOAD).build());

        // Add it to the list of addons
        if (addons.contains(addon)) {
            addons.remove(addon);
        }
        addons.add(addon);

        // Add to the list of loaders
        loaders.put(addon, addonClassLoader);

        try {
            // Run the onLoad.
            addon.onLoad();
            // if game mode, get the world name and generate
            if (addon instanceof GameModeAddon) {
                GameModeAddon gameMode = (GameModeAddon) addon;
                if (!gameMode.getWorldSettings().getWorldName().isEmpty()) {
                    worldNames.put(gameMode.getWorldSettings().getWorldName(), gameMode);
                }
            }
            // Addon successfully loaded
            addon.setState(Addon.State.LOADED);
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(addon);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(addon, e);
        }
    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        if (!getLoadedAddons().isEmpty()) {
            plugin.log("Enabling addons...");
            getLoadedAddons().forEach(this::enableAddon);
            plugin.log("Addons successfully enabled.");
        }
    }

    /**
     * Enables an addon
     * @param addon addon
     */
    private void enableAddon(Addon addon) {
        try {
            // If this is a GameModeAddon create the worlds, register it and load the blueprints
            if (addon instanceof GameModeAddon) {
                GameModeAddon gameMode = (GameModeAddon) addon;
                // Create the gameWorlds
                gameMode.createWorlds();
                plugin.getIWM().addGameMode(gameMode);
                // Save and load blueprints
                plugin.getBlueprintsManager().extractDefaultBlueprints(gameMode);
                plugin.getBlueprintsManager().loadBlueprintBundles(gameMode);
            }
            addon.onEnable();
            if (addon instanceof GameModeAddon) {
                GameModeAddon gameMode = (GameModeAddon) addon;
                // Set the worlds for the commands
                gameMode.getPlayerCommand().ifPresent(c -> c.setWorld(gameMode.getOverWorld()));
                gameMode.getAdminCommand().ifPresent(c -> c.setWorld(gameMode.getOverWorld()));
            }
            Bukkit.getPluginManager().callEvent(new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build());
            addon.setState(Addon.State.ENABLED);
            plugin.log("Enabling " + addon.getDescription().getName() + "...");
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchFieldError e) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            handleAddonIncompatibility(addon);
        } catch (Exception e) {
            // Unhandled exception. We'll give a bit of debug here.
            handleAddonError(addon, e);
        }
    }

    /**
     * Handles an addon which failed to load due to an incompatibility (missing class, missing method).
     * @param addon instance of the Addon.
     * @since 1.1
     */
    private void handleAddonIncompatibility(@NonNull Addon addon) {
        // Set the AddonState as "INCOMPATIBLE".
        addon.setState(Addon.State.INCOMPATIBLE);
        plugin.log("Skipping " + addon.getDescription().getName() + " as it is incompatible with the current version of BentoBox or of server software...");
        plugin.log("NOTE: The addon is referring to no longer existing classes.");
        plugin.log("NOTE: DO NOT report this as a bug from BentoBox.");
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
        loadAddons();
        enableAddons();
    }

    /**
     * Reloads one addon
     * @param addon - addon
     */
    public void reloadAddon(Addon addon) {
        Path p = addon.getFile().toPath();
        disable(addon);
        loadAddon(p.toFile());
        enableAddon(addon);
    }

    /**
     * Gets the addon by name
     * @param name addon name, not null
     * @return Optional addon object
     */
    @NonNull
    public Optional<Addon> getAddonByName(@NonNull String name){
        return addons.stream().filter(a -> a.getDescription().getName().equalsIgnoreCase(name)).findFirst();
    }

    @NonNull
    private YamlConfiguration addonDescription(@NonNull JarFile jar) throws InvalidAddonFormatException, IOException, InvalidConfigurationException {
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
        return data;
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        if (!getEnabledAddons().isEmpty()) {
            plugin.log("Disabling addons...");
            // Disable addons
            getEnabledAddons().forEach(this::disable);
            plugin.log("Addons successfully disabled.");
        }
        // Clear all maps
        listeners.clear();
        addons.clear();
        loaders.clear();
        classes.clear();
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
        return getEnabledAddons().stream()
                .filter(GameModeAddon.class::isInstance)
                .map(GameModeAddon.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of Addons that are loaded.
     * @return list of loaded Addons.
     * @since 1.1
     */
    @NonNull
    public List<Addon> getLoadedAddons() {
        return addons.stream().filter(addon -> addon.getState().equals(Addon.State.LOADED)).collect(Collectors.toList());
    }

    /**
     * Gets the list of Addons that are enabled.
     * @return list of enabled Addons.
     * @since 1.1
     */
    @NonNull
    public List<Addon> getEnabledAddons() {
        return addons.stream().filter(addon -> addon.getState().equals(Addon.State.ENABLED)).collect(Collectors.toList());
    }

    @Nullable
    public AddonClassLoader getLoader(@NonNull final Addon addon) {
        return loaders.get(addon);
    }

    /**
     * Finds a class by name that has been loaded by this loader
     * @param name name of the class, not null
     * @return Class the class
     */
    @Nullable
    public Class<?> getClassByName(@NonNull final String name) {
        return classes.getOrDefault(name, loaders.values().stream().map(l -> l.findClass(name, false)).filter(Objects::nonNull).findFirst().orElse(null));
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

    private void sortAddons() {
        // Lists all available addons as names.
        List<String> names = addons.stream().map(a -> a.getDescription().getName()).collect(Collectors.toList());

        // Check that any dependencies exist
        Iterator<Addon> addonsIterator = addons.iterator();
        while (addonsIterator.hasNext()) {
            Addon a = addonsIterator.next();
            for (String dependency : a.getDescription().getDependencies()) {
                if (!names.contains(dependency)) {
                    plugin.logError(a.getDescription().getName() + " has dependency on " + dependency + " that does not exist. Addon will not load!");
                    addonsIterator.remove();
                    break;
                }
            }
        }

        // Load dependencies or soft dependencies
        Map<String,Addon> sortedAddons = new LinkedHashMap<>();
        // Start with nodes with no dependencies
        addons.stream().filter(a -> a.getDescription().getDependencies().isEmpty() && a.getDescription().getSoftDependencies().isEmpty())
        .forEach(a -> sortedAddons.put(a.getDescription().getName(), a));
        // Fill remaining
        List<Addon> remaining = addons.stream().filter(a -> !sortedAddons.containsKey(a.getDescription().getName())).collect(Collectors.toList());

        // Run through remaining addons
        remaining.forEach(addon -> {
            // Get the addon's dependencies.
            List<String> dependencies = new ArrayList<>(addon.getDescription().getDependencies());
            dependencies.addAll(addon.getDescription().getSoftDependencies());

            // Remove already sorted addons (dependencies) from the list
            dependencies.removeIf(sortedAddons::containsKey);

            if (dependencies.stream().noneMatch(dependency -> addon.getDescription().getDependencies().contains(dependency))) {
                sortedAddons.put(addon.getDescription().getName(), addon);
            }
        });

        addons.clear();
        addons.addAll(sortedAddons.values());
    }

    /**
     * Get the world generator if it exists
     * @param worldName - name of world
     * @param id - specific generator id
     * @return ChunkGenerator or null if none found
     * @since 1.2.0
     */
    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        // Clean up world name
        String w = worldName.replace("_nether", "").replace("_the_end", "");
        if (worldNames.containsKey(w)) {
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
            addon.onDisable();
            Bukkit.getPluginManager().callEvent(new AddonEvent().builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build());
            plugin.log("Disabling " + addon.getDescription().getName() + "...");
        }
        // Clear loaders
        if (loaders.containsKey(addon)) {
            try {
                loaders.get(addon).close();
            } catch (IOException ignore) {
                // Nothing
            }
        }

        // Remove it from the addons list
        addons.remove(addon);
    }

    /*
     * Get a list of addon classes that are of type {@link DataObject}
     * but not {@link ConfigObject}. Configs are not transitioned to database.
     * Used in database transition.
     * @return list of DataObjects
     * @since 1.5.0
     */
    public List<Class<?>> getDataObjects() {
        return classes.values().stream()
                .filter(DataObject.class::isAssignableFrom)
                // Do not include config files
                .filter(c -> !ConfigObject.class.isAssignableFrom(c))
                .collect(Collectors.toList());
    }

}
