package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.blueprints.Blueprint;
import world.bentobox.bentobox.api.blueprints.BlueprintBundle;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.util.Util;

/**
 * Handles {@link world.bentobox.bentobox.api.blueprints.BlueprintDO Blueprints}.
 * @since 1.5.0
 * @author Poslovitch, tastybento
 */
public class BlueprintsManager {

    public static final @NonNull String FOLDER_NAME = "blueprints";

    private static final String BLUEPRINT_BUNDLE_FILE_EXTENSION = ".json";
    private static final String BLUEPRINT_FILE_EXTENSION = ".bp";

    private @NonNull BentoBox plugin;

    private @NonNull Map<GameModeAddon, List<BlueprintBundle>> blueprintBundles;
    private @NonNull Map<GameModeAddon, List<Blueprint>> blueprints;

    private Gson gson;


    public BlueprintsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.blueprintBundles = new HashMap<>();
        getGson();
    }

    private void getGson() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization();
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
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_BUNDLE_FILE_EXTENSION).forEach(name -> addon.saveResource(name, false));
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_FILE_EXTENSION).forEach(name -> addon.saveResource(name, false));
        } catch (IOException e) {
            plugin.logError("Could not load schem files from addon jar " + e.getMessage());
        }
    }

    /**
     * Loads the blueprint bundles of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprintBundles(@NonNull GameModeAddon addon) {
        blueprintBundles.putIfAbsent(addon, new ArrayList<>());
        File bpf = getBlueprintsFolder(addon);
        for (File file: Objects.requireNonNull(bpf.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_BUNDLE_FILE_EXTENSION)))) {
            try {
                blueprintBundles.get(addon).add(gson.fromJson(new FileReader(file), BlueprintBundle.class));
            } catch (Exception e) {
                plugin.logError("Could not load blueprint bundle " + file.getName() + " " + e.getMessage());
            }
        }
    }

    /**
     * Loads all the blueprints of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprints(@NonNull GameModeAddon addon) {
        File bpf = getBlueprintsFolder(addon);
        for (File file: Objects.requireNonNull(bpf.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_FILE_EXTENSION)))) {
            try {
                blueprints.computeIfAbsent(addon, k -> new ArrayList<>()).add(gson.fromJson(new FileReader(file), Blueprint.class));
            } catch (Exception e) {
                plugin.logError("Could not load blueprint " + file.getName() + " " + e.getMessage());
            }
        }
    }

    /**
     * Load a blueprint by filename
     * @param addon - game mode addon
     * @param fileName - filename
     * @throws JsonSyntaxException
     * @throws JsonIOException
     * @throws FileNotFoundException
     */
    public void loadBlueprint(@NonNull GameModeAddon addon, String fileName) {
        try {
            blueprints.computeIfAbsent(addon, k -> new ArrayList<>()).add(gson.fromJson(new FileReader(new File(getBlueprintsFolder(addon), fileName)), Blueprint.class));
        } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
            plugin.logError("Could not load blueprint " + fileName + " " + e.getMessage());
        }
    }

    /**
     * Saves all the blueprint bundles
     */
    public void saveBlueprintBundles() {
        blueprintBundles.forEach(this::saveBlueprintBundle);
    }

    /**
     * Save blueprint bundles for game mode
     * @param addon - gamemode addon
     * @param bundleList - list of bundles
     */
    public void saveBlueprintBundle(GameModeAddon addon, List<BlueprintBundle> bundleList) {
        File bpf = getBlueprintsFolder(addon);
        for (BlueprintBundle bb : bundleList) {
            File fileName = new File(bpf, bb.getUniqueId() + "." + BLUEPRINT_BUNDLE_FILE_EXTENSION);
            String toStore = gson.toJson(bb, BlueprintBundle.class);
            try (FileWriter fileWriter = new FileWriter(fileName)) {
                fileWriter.write(toStore);
            } catch (IOException e) {
                plugin.logError("Could not save blueprint bundle file: " + e.getMessage());
            }
        }
    }

    /**
     * Save blueprint for game mode
     * @param addon - gamemode addon
     * @param blueprint - blueprint
     */
    public void saveBlueprint(File bpf, Blueprint blueprint) {
        File fileName = new File(bpf, blueprint.getName() + "." + BLUEPRINT_BUNDLE_FILE_EXTENSION);
        String toStore = gson.toJson(blueprint, Blueprint.class);
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(toStore);
        } catch (IOException e) {
            plugin.logError("Could not save blueprint file: " + e.getMessage());
        }
    }

    /**
     * Gets the blueprint bundles of this addon.
     * @param addon the {@link GameModeAddon} to get the blueprint bundles.
     */
    public List<BlueprintBundle> getBlueprintBundles(@NonNull GameModeAddon addon) {
        return blueprintBundles.getOrDefault(addon, new ArrayList<>());
    }

    /**
     * Set the bundles for this addon
     * @param addon - {@link GameModeAddon}
     * @param list - list of bundles
     */
    public void setBlueprintBundles(@NonNull GameModeAddon addon, List<BlueprintBundle> list) {
        blueprintBundles.put(addon, list);
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


}
