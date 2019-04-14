package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.blueprints.Blueprint;
import world.bentobox.bentobox.api.blueprints.BlueprintBundle;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipFile;

/**
 * Handles {@link world.bentobox.bentobox.api.blueprints.Blueprint Blueprints}.
 * @since 1.5.0
 * @author Poslovitch
 */
public class BlueprintsManager {

    public static final @NonNull String FOLDER_NAME = "blueprints";

    private @NonNull BentoBox plugin;

    private @NonNull Map<GameModeAddon, List<Blueprint>> blueprints;
    private @NonNull Map<GameModeAddon, List<BlueprintBundle>> blueprintBundles;

    public BlueprintsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.blueprints = new HashMap<>();
        this.blueprintBundles = new HashMap<>();
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
        // TODO
    }

    /**
     * Loads the blueprints of this addon from its blueprints folder.
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprints(@NonNull GameModeAddon addon) {
        File folder = getBlueprintsFolder(addon);
        // Create the empty list if not already there
        blueprints.putIfAbsent(addon, new LinkedList<>());
        // Look through the folder
        FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith("." + Blueprint.FILE_EXTENSION);
        Arrays.stream(Objects.requireNonNull(folder.list(filter))).map(name -> name.split("\\.")[0]).forEach(name -> loadBlueprint(addon, name));
    }

    /**
     * Loads the blueprint of this addon corresponding to this name.
     * @param addon the {@link GameModeAddon} to load the blueprint from.
     * @param name the name of the blueprint to load, without the file extension.
     *             E.g: {@code "island"}.
     */
    public void loadBlueprint(@NonNull GameModeAddon addon, @NonNull String name) {
        File folder = getBlueprintsFolder(addon);
        // Create the empty list if not already there
        blueprints.putIfAbsent(addon, new LinkedList<>());
        // Load the blueprint
        plugin.log("Loading " + name + ".blueprint for " + addon.getDescription().getName());
        try {
            Blueprint blueprint = new Blueprint(name, new ZipFile(new File(folder, name + "." + Blueprint.FILE_EXTENSION)));
            blueprints.get(addon).add(blueprint);
        } catch (Exception e) {
            // TODO: add error debug
        }
    }

    @NonNull
    public Map<GameModeAddon, List<Blueprint>> getBlueprints() {
        return blueprints;
    }

    @NonNull
    public List<Blueprint> getBlueprints(@NonNull GameModeAddon addon) {
        return blueprints.getOrDefault(addon, new LinkedList<>());
    }

    @NonNull
    public Map<GameModeAddon, List<BlueprintBundle>> getBlueprintBundles() {
        return blueprintBundles;
    }

    @NonNull
    public List<BlueprintBundle> getBlueprintBundles(@NonNull GameModeAddon addon) {
        return blueprintBundles.getOrDefault(addon, new LinkedList<>());
    }

    /**
     * Returns a {@link File} instance of the blueprints folder of this {@link GameModeAddon}.
     * @param addon the {@link GameModeAddon}
     * @return a {@link File} instance of the blueprints folder of this GameModeAddon.
     */
    @NonNull
    public File getBlueprintsFolder(@NonNull GameModeAddon addon) {
        return new File(addon.getDataFolder(), FOLDER_NAME);
    }
}
