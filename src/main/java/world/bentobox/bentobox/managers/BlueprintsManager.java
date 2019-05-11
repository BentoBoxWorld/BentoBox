package world.bentobox.bentobox.managers;

import java.io.File;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
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

    private static final String FILE_EXTENSION = "json";

    private @NonNull BentoBox plugin;

    private @NonNull Map<GameModeAddon, List<BlueprintBundle>> blueprintBundles;

    private Gson gson;


    public BlueprintsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.blueprintBundles = new HashMap<>();
        getGson();
    }

    private void getGson() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization().setPrettyPrinting();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(plugin));
        // Keep null in the database
        builder.serializeNulls();
        // Allow characters like < or > without escaping them
        builder.disableHtmlEscaping();
        gson = builder.create();
    }

    /**
     * Extracts the blueprints provided by this {@link GameModeAddon} in its .jar file.
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

        // Get any blueprints from the jar and save them.
        // Save any schems that are in the jar
        try (JarFile jar = new JarFile(addon.getFile())) {
            Util.listJarFiles(jar, FOLDER_NAME, FILE_EXTENSION).forEach(name -> addon.saveResource(name, false));
        } catch (IOException e) {
            plugin.logError("Could not load schem files from addon jar " + e.getMessage());
        }
    }

    /**
     * Loads the blueprints of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprintBundles(@NonNull GameModeAddon addon) {
        blueprintBundles.putIfAbsent(addon, new ArrayList<>());
        File bpf = getBlueprintsFolder(addon);
        for (File file: Objects.requireNonNull(bpf.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(FILE_EXTENSION)))) {
            try {
                blueprintBundles.get(addon).add(gson.fromJson(new FileReader(file), BlueprintBundle.class));
            } catch (Exception e) {
                plugin.logError("Could not load objects " + file.getName() + " " + e.getMessage());
            }
        }
        // Create a cache of blueprints
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
            File fileName = new File(bpf, bb.getUniqueId() + "." + FILE_EXTENSION);
            String toStore = gson.toJson(bb, BlueprintBundle.class);
            try (FileWriter fileWriter = new FileWriter(fileName)) {
                fileWriter.write(toStore);
            } catch (IOException e) {
                plugin.logError("Could not save blueprint file: " + e.getMessage());
            }
        }
    }

    // Blueprint adding etc.


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
