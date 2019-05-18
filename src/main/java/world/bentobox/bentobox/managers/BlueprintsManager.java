package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.schems.SchemToBlueprint;
import world.bentobox.bentobox.util.Util;

/**
 * Handles Blueprints
 * @since 1.5.0
 * @author Poslovitch, tastybento
 */
public class BlueprintsManager {

    private static final String BLUEPRINT_BUNDLE_SUFFIX = ".json";
    public static final String BLUEPRINT_SUFFIX = ".blu";
    public static final String DEFAULT_BUNDLE_NAME = "default";

    public static final @NonNull String FOLDER_NAME = "blueprints";

    /**
     * Map of blueprint bundles to game mode addon.
     * Inner map's key is the uniqueId of the blueprint bundle so it's
     * easy to get from a UI
     */
    private @NonNull Map<GameModeAddon, Map<String, BlueprintBundle>> blueprintBundles;

    /**
     * Map of blueprints. There can be many blueprints per game mode addon
     * Inner map's key is the blueprint's name so it's easy to get from a UI
     */
    private @NonNull Map<GameModeAddon, Map<String, Blueprint>> blueprints;

    /**
     * Gson used for serializing/deserializing the bundle class
     */
    private final Gson gson;

    private @NonNull BentoBox plugin;


    public BlueprintsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.blueprintBundles = new HashMap<>();
        this.blueprints = new HashMap<>();
        @SuppressWarnings("rawtypes")
        GsonBuilder builder = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        // This enables gson to deserialize enum maps
        .registerTypeAdapter(EnumMap.class, new InstanceCreator<EnumMap>() {
            @SuppressWarnings("unchecked")
            @Override
            public EnumMap createInstance(Type type) {
                Type[] types = (((ParameterizedType) type).getActualTypeArguments());
                return new EnumMap((Class<?>) types[0]);
            }
        });
        // Disable <>'s escaping etc.
        builder.disableHtmlEscaping();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(plugin));
        gson = builder.create();
    }

    /**
     * Extracts the blueprints and bundles provided by this {@link GameModeAddon} in its .jar file.
     * This will do nothing if the blueprints folder already exists for this GameModeAddon.
     * @param addon the {@link GameModeAddon} to extract the blueprints from.
     */
    public void extractDefaultBlueprints(@NonNull GameModeAddon addon) {
        File folder = getBlueprintsFolder(addon);
        if (folder.exists()) {
            // If the folder exists, do not copy anything from the jar
            return;
        }

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.logError("Could not create the '" + FOLDER_NAME + "' folder!");
            plugin.logError("This might be due to incorrectly set-up write permissions on the operating system.");
            return;
        }

        // Get any blueprints or bundles from the jar and save them.
        try (JarFile jar = new JarFile(addon.getFile())) {
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_BUNDLE_SUFFIX).forEach(name -> addon.saveResource(name, false));
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_SUFFIX).forEach(name -> addon.saveResource(name, false));
        } catch (IOException e) {
            plugin.logError("Could not load blueprint files from addon jar " + e.getMessage());
        }
    }

    /**
     * Get the blueprint bundles of this addon.
     * @param addon the {@link GameModeAddon} to get the blueprint bundles.
     */
    public Map<String, BlueprintBundle> getBlueprintBundles(@NonNull GameModeAddon addon) {
        return blueprintBundles.getOrDefault(addon, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Returns a {@link File} instance of the blueprints folder of this {@link GameModeAddon}.
     * @param addon the {@link GameModeAddon}
     * @return a {@link File} instance of the blueprints folder of this GameModeAddon.
     */
    @NonNull
    private File getBlueprintsFolder(@NonNull GameModeAddon addon) {
        return new File(addon.getDataFolder(), FOLDER_NAME);
    }

    /**
     * Loads the blueprint bundles of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprintBundles(@NonNull GameModeAddon addon) {
        blueprintBundles.put(addon, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

        // See if there are any schems that need converting
        new SchemToBlueprint(plugin).convertSchems(addon);

        if (!loadBundles(addon)) {
            makeDefaults(addon);
            loadBundles(addon);
        }
        // Load blueprints
        loadBlueprints(addon);
    }

    private boolean loadBundles(@NonNull GameModeAddon addon) {
        File bpf = getBlueprintsFolder(addon);
        boolean loaded = false;
        for (File file: Objects.requireNonNull(bpf.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_BUNDLE_SUFFIX)))) {
            try {
                BlueprintBundle bb = gson.fromJson(new FileReader(file), BlueprintBundle.class);
                blueprintBundles.get(addon).put(bb.getUniqueId(), bb);
                plugin.log("Loaded Blueprint Bundle '" + bb.getUniqueId() + "' for " + addon.getDescription().getName());
                loaded = true;
            } catch (Exception e) {
                plugin.logError("Could not load blueprint bundle " + file.getName() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
        return loaded;
    }

    /**
     * This should never be needed and is just a boot strap
     * @param addon
     */
    private void makeDefaults(@NonNull GameModeAddon addon) {
        plugin.logError("No blueprint bundles found! Creating a default one.");
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId(DEFAULT_BUNDLE_NAME);
        bb.setDisplayName("Default bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "Default bundle of blueprints"));
        // Default blueprints
        Blueprint defaultBp = new Blueprint();
        defaultBp.setName("bedrock");
        defaultBp.setDescription(Collections.singletonList(ChatColor.AQUA + "A bedrock block"));
        defaultBp.setBedrock(new Vector(0,0,0));
        Map<Vector, BlueprintBlock> map = new HashMap<>();
        map.put(new Vector(0,0,0), new BlueprintBlock("minecraft:bedrock"));
        defaultBp.setBlocks(map);
        // Save a default "bedrock" blueprint
        new BlueprintClipboardManager(plugin, getBlueprintsFolder(addon)).saveBlueprint(defaultBp);
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        blueprintBundles.get(addon).put(DEFAULT_BUNDLE_NAME, bb);
        this.saveBlueprintBundles();
    }

    /**
     * Loads all the blueprints of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprints(@NonNull GameModeAddon addon) {
        blueprints.put(addon, new HashMap<>());
        File bpf = getBlueprintsFolder(addon);
        for (File file: Objects.requireNonNull(bpf.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_SUFFIX)))) {
            String fileName = file.getName().substring(0, file.getName().length() - BLUEPRINT_SUFFIX.length());
            try {
                Blueprint bp = new BlueprintClipboardManager(plugin, bpf).loadBlueprint(fileName);
                blueprints.get(addon).put(bp.getName(), bp);
                plugin.log("Loaded blueprint '" + bp.getName() + "' for " + addon.getDescription().getName());
            } catch (Exception e) {
                plugin.logError("Could not load blueprint " + fileName + " " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a blueprint to addon's list of blueprints
     * @param addon - the {@link GameModeAddon}
     * @param bp - blueprint
     */
    public void addBlueprint(@NonNull GameModeAddon addon, @NonNull Blueprint bp) {
        blueprints.putIfAbsent(addon, new HashMap<>());
        blueprints.get(addon).put(bp.getName(), bp);
        plugin.log("Added blueprint '" + bp.getName() + "' for " + addon.getDescription().getName());
    }

    /**
     * Saves a blueprint into addon's blueprint folder
     * @param addon - the {@link GameModeAddon}
     * @param bp - blueprint to save
     */
    public boolean saveBlueprint(@NonNull GameModeAddon addon, @NonNull Blueprint bp) {
        return new BlueprintClipboardManager(plugin, getBlueprintsFolder(addon)).saveBlueprint(bp);
    }

    /**
     * Save blueprint bundles for game mode
     * @param addon - gamemode addon
     * @param bundleList - list of bundles
     */
    public void saveBlueprintBundle(GameModeAddon addon, BlueprintBundle bb) {
        File bpf = getBlueprintsFolder(addon);
        File fileName = new File(bpf, bb.getUniqueId() + BLUEPRINT_BUNDLE_SUFFIX);
        String toStore = gson.toJson(bb, BlueprintBundle.class);
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(toStore);
        } catch (IOException e) {
            plugin.logError("Could not save blueprint bundle file: " + e.getMessage());
        }
    }

    /**
     * Saves all the blueprint bundles
     */
    public void saveBlueprintBundles() {
        blueprintBundles.forEach((k,v) -> v.values().forEach(m -> saveBlueprintBundle(k, m)));
    }

    /**
     * Set the bundles for this addon
     * @param addon - {@link GameModeAddon}
     * @param map - map of bundles, key is the bundle unique id, value is the bundle
     */
    public void setBlueprintBundles(@NonNull GameModeAddon addon, Map<String, BlueprintBundle> map) {
        blueprintBundles.put(addon, map);
    }

    /**
     * @return the blueprints
     */
    public Map<GameModeAddon, Map<String, Blueprint>> getBlueprints() {
        return blueprints;
    }

    /**
     * Paste the islands to world
     * @param addon - GameModeAddon
     * @param island - island
     * @param name - bundle name
     */
    public void paste(GameModeAddon addon, Island island, String name) {
        paste(addon, island, name, null);
    }

    /**
     * Paste islands to the world and run task afterwards
     * @param addon - the game mode addon
     * @param island - the island
     * @param name - name of bundle to paste
     * @param task - task to run after pasting is completed
     * @return true if okay, false is there is a problem
     */
    public boolean paste(GameModeAddon addon, Island island, String name, Runnable task) {
        if (validate(addon, name) == null) {
            plugin.logError("Tried to paste '" + name + "' but the bundle is not loaded!");
            return false;
        }
        BlueprintBundle bb = blueprintBundles.get(addon).get(name.toLowerCase(Locale.ENGLISH));
        if (!blueprints.containsKey(addon) || blueprints.get(addon).isEmpty()) {
            plugin.logError("No blueprints loaded for bundle '" + name + "'!");
            return false;
        }
        Blueprint bp = blueprints.get(addon).get(bb.getBlueprint(World.Environment.NORMAL));
        // Paste overworld
        new BlueprintPaster(plugin, bp, addon.getOverWorld(), island, task);
        // Make nether island
        if (bb.getBlueprint(World.Environment.NETHER) != null
                && addon.getWorldSettings().isNetherGenerate()
                && addon.getWorldSettings().isNetherIslands()
                && addon.getNetherWorld() != null) {
            bp = blueprints.get(addon).get(bb.getBlueprint(World.Environment.NETHER));
            new BlueprintPaster(plugin, bp, addon.getNetherWorld(), island, null);
        }
        // Make end island
        if (bb.getBlueprint(World.Environment.THE_END) != null
                && addon.getWorldSettings().isEndGenerate()
                && addon.getWorldSettings().isEndIslands()
                && addon.getEndWorld() != null) {
            bp = blueprints.get(addon).get(bb.getBlueprint(World.Environment.THE_END));
            new BlueprintPaster(plugin, bp, addon.getEndWorld(), island, null);
        }
        return true;

    }

    /**
     * Validate if the bundle name is valid or not
     * @param addon - game mode addon
     * @param name - bundle name
     * @return bundle name or null if it's invalid
     */
    public @Nullable String validate(GameModeAddon addon, String name) {
        if (name == null) {
            return null;
        }
        if (blueprintBundles.containsKey(addon) && blueprintBundles.get(addon).containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return name;
        }
        return null;
    }

    /**
     * Adds a blueprint bundle
     * @param addon - the game mode addon
     * @param bb - the blueprint bundle
     */
    public void addBlueprintBundle(GameModeAddon addon, BlueprintBundle bb) {
        blueprintBundles.computeIfAbsent(addon, k -> new HashMap<>()).put(bb.getUniqueId(), bb);

    }

    /**
     * Shows a player a panel of selectable blueprint bundles. Checks user's permission
     * @param command - the command requesting the panel, e.g., create or reset
     * @param user - the user
     * @param label - label
     */
    public void showPanel(CompositeCommand command, User user, String label) {
        // Create the panel
        PanelBuilder pb = new PanelBuilder().name(user.getTranslation("commands.island.create.pick")).user(user);
        // Get the bundles
        Collection<BlueprintBundle> bbs = getBlueprintBundles((@NonNull GameModeAddon) command.getAddon()).values();
        // Loop through them and create items in the panel
        for (BlueprintBundle bb : bbs) {
            String perm = command.getPermissionPrefix() + "island.create." + bb.getUniqueId();
            if (!bb.getUniqueId().equals(BlueprintsManager.DEFAULT_BUNDLE_NAME)
                    && bb.isRequirePermission()
                    && !user.hasPermission(perm)) {
                // Skip bundles that the user has no permission for
                continue;
            }
            PanelItem pi = new PanelItemBuilder().name(bb.getDisplayName()).description(bb.getDescription())
                    .icon(bb.getIcon()).name(bb.getUniqueId()).clickHandler((panel, user1, clickType, slot1) -> {
                        user1.closeInventory();
                        command.execute(user1, label, Collections.singletonList(bb.getUniqueId()));
                        return true;
                    }).build();
            pb.item(pi);
        }
        pb.build();
    }

    /**
     * Checks if a player has permission to see or use this blueprint bundle.
     * @param addon - addon making the request
     * @param user - user making the request
     * @param name - name of the blueprint bundle
     * @return <tt>true</tt> if allowed
     */
    public boolean checkPerm(Addon addon, User user, String name) {
        // Permission
        String permission = addon.getPermissionPrefix() + "island.create." + name;
        // Get Blueprint bundle
        BlueprintBundle bb = getBlueprintBundles((GameModeAddon)addon).get(name.toLowerCase(Locale.ENGLISH));
        if (bb == null || (bb.isRequirePermission() && !name.equals(DEFAULT_BUNDLE_NAME) && !user.hasPermission(permission))) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, permission);
            return false;
        }
        return true;
    }

}
