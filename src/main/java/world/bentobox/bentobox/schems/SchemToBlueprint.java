package world.bentobox.bentobox.schems;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;

public class SchemToBlueprint {

    public static final @NonNull String DEFAULT_SCHEM_NAME = "island";
    public static final @NonNull String FILE_EXTENSION = ".schem";
    public static final @NonNull String FOLDER_NAME = "schems";

    private BentoBox plugin;

    /**
     * @param plugin - plugin
     */
    public SchemToBlueprint(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Converts schems to blueprints and blueprint bundles
     * @param addon - GameModeAddon
     */
    public void convertSchems(GameModeAddon addon) {
        File schems = new File(addon.getDataFolder(), FOLDER_NAME);
        if (!schems.exists()) {
            return;
        }
        // Convert all schems in folder
        // Look through the folder
        FilenameFilter schemFilter = (File dir, String name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(FILE_EXTENSION)
                && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("nether-")
                && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("end-");

        Arrays.stream(Objects.requireNonNull(schems.list(schemFilter)))
        .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
        .forEach(name -> importSchemSet(addon, schems, name));

        File newDir = new File(addon.getDataFolder(), FOLDER_NAME + "_converted");
        try {
            Files.move(schems.toPath(), newDir.toPath());
        } catch (IOException e) {
            plugin.logError("Could not move schems folder: " + e.getLocalizedMessage());
        }
    }

    /**
     * Imports one schem set to the game mode
     * @param addon - game mode addon
     * @param schems
     * @param name
     */
    private void importSchemSet(GameModeAddon addon, File schems, String name) {
        // Make a new blueprint bundle
        BlueprintBundle bb = new BlueprintBundle();
        // TODO: This is just placeholder text
        if (name.equalsIgnoreCase("island")) {
            bb.setUniqueId(BlueprintsManager.DEFAULT_BUNDLE_NAME);
            bb.setDisplayName(ChatColor.YELLOW + "The Original");
            bb.setDescription(ChatColor.AQUA + "Standard set of islands");
            bb.setIcon(Material.GRASS);
        } else {
            bb.setUniqueId(name);
            bb.setDisplayName(name + " island");
            bb.setIcon(Material.GRASS_PATH);
        }
        Blueprint bp = loadSchemSaveBlueprint(addon, schems, name);
        if (bp != null) {
            bb.setBlueprint(World.Environment.NORMAL, bp);
            plugin.getBlueprintsManager().saveBlueprint(addon, bp);
            bb.setDescription(ChatColor.GREEN + "Includes an Overworld island");
        }
        bp = loadSchemSaveBlueprint(addon, schems, "nether-" + name);
        if (bp != null) {
            bb.setBlueprint(World.Environment.NETHER, bp);
            plugin.getBlueprintsManager().saveBlueprint(addon, bp);
            bb.setDescription(ChatColor.RED + "Includes a Nether island");
        }
        bp = loadSchemSaveBlueprint(addon, schems, "end-" + name);
        if (bp != null) {
            bb.setBlueprint(World.Environment.THE_END, bp);
            plugin.getBlueprintsManager().saveBlueprint(addon, bp);
            bb.setDescription(ChatColor.GOLD + "Includes an End island");
        }
        // Add it to the blueprint manager
        plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);

        // Done!
    }

    private Blueprint loadSchemSaveBlueprint(GameModeAddon addon, File schems, String name) {
        try {
            SchemLoader loader = new SchemLoader(plugin, schems);
            loader.load(name);
            plugin.log("Loaded " + name + ".schem");
            // Convert blueprint
            plugin.log("Converting " + name + ".schem to a blueprint");
            Blueprint bp = new Converter().convert(loader.getBlockConfig());
            bp.setName(name);
            plugin.log("Saving blueprint");
            plugin.getBlueprintsManager().saveBlueprint(addon, bp);
            return bp;
        } catch (FileNotFoundException ignore) {
            // Ignore
        } catch (Exception e) {
            plugin.logError("Could not convert " + name + " schem, skipping!");
            e.printStackTrace();
        }
        return null;
    }

}
