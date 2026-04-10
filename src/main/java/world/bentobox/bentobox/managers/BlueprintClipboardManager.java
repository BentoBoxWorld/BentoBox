package world.bentobox.bentobox.managers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintClipboardManager {

    private static final String LOAD_ERROR = "Could not load blueprint file - does not exist : ";

    private final File blueprintFolder;

    private BlueprintClipboard clipboard;

    private Gson gson;

    private final BentoBox plugin;

    public BlueprintClipboardManager(BentoBox plugin, File blueprintFolder) {
        this(plugin, blueprintFolder, null);
    }

    public BlueprintClipboardManager(BentoBox plugin, File blueprintFolder, BlueprintClipboard clipboard) {
        super();
        this.plugin = plugin;
        if (!blueprintFolder.exists()) {
            blueprintFolder.mkdirs();
        }
        this.blueprintFolder = blueprintFolder;
        this.clipboard = clipboard;
        getGson();
    }

    /**
     * @return the clipboard
     */
    public BlueprintClipboard getClipboard() {
        return clipboard;
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
     * Load a file to clipboard
     * @param fileName - filename in blueprints folder
     * @throws IOException - if there's a load error with unzipping or name
     */
    public void load(String fileName) throws IOException {
        clipboard = new BlueprintClipboard(loadBlueprint(fileName));

    }

    /**
     * Loads a blueprint
     * @param fileName - the sanitized filename without the suffix
     * @return the blueprint
     * @throws IOException exception if there's an issue loading
     */
    public Blueprint loadBlueprint(String fileName) throws IOException {
        // Try the new plain JSON format first (.blueprint)
        File jsonFile = new File(blueprintFolder, fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
        if (jsonFile.exists()) {
            return loadBlueprintFromJson(jsonFile, fileName);
        }
        // Fall back to legacy zipped format (.blu)
        File zipFile = new File(blueprintFolder, fileName + BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX);
        if (zipFile.exists()) {
            return loadBlueprintFromZip(zipFile, fileName);
        }
        plugin.logError(LOAD_ERROR + fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
        throw new IOException(LOAD_ERROR + fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
    }

    private Blueprint loadBlueprintFromJson(File jsonFile, String fileName) throws IOException {
        Blueprint bp;
        try (FileReader fr = new FileReader(jsonFile, StandardCharsets.UTF_8)) {
            bp = gson.fromJson(fr, Blueprint.class);
        } catch (Exception e) {
            plugin.logError("Blueprint has JSON error: " + jsonFile.getName());
            plugin.logStacktrace(e);
            throw new IOException("Blueprint has JSON error: " + jsonFile.getName());
        }
        return checkBedrock(bp, fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
    }

    private Blueprint loadBlueprintFromZip(File zipFile, String fileName) throws IOException {
        unzip(zipFile.getCanonicalPath());
        File file = new File(blueprintFolder, fileName);
        if (!file.exists()) {
            plugin.logError(LOAD_ERROR + file.getName());
            throw new IOException(LOAD_ERROR + file.getName() + " temp file");
        }
        Blueprint bp;
        try (FileReader fr = new FileReader(file, StandardCharsets.UTF_8)) {
            bp = gson.fromJson(fr, Blueprint.class);
        } catch (Exception e) {
            plugin.logError("Blueprint has JSON error: " + zipFile.getName());
            plugin.logStacktrace(e);
            throw new IOException("Blueprint has JSON error: " + zipFile.getName());
        }
        Files.delete(file.toPath());
        return checkBedrock(bp, fileName + BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX);
    }

    private Blueprint checkBedrock(Blueprint bp, String fileName) {
        if (bp.getBedrock() == null) {
            bp.setBedrock(new Vector(bp.getxSize() / 2, bp.getySize() / 2, bp.getzSize() / 2));
            bp.getBlocks().put(bp.getBedrock(), new BlueprintBlock(Material.BEDROCK.createBlockData().getAsString()));
            plugin.logWarning("Blueprint " + fileName + " had no bedrock block in it so one was added automatically in the center. You should check it.");
        }
        return bp;
    }

    /**
     * Load a blueprint to the clipboard for a user
     * @param user - user trying to load
     * @param fileName - filename
     * @return - <tt>true</tt> if load is successful, <tt>false</tt> if not
     */
    public boolean load(User user, String fileName) {
        try {
            load(fileName);
        } catch (IOException e1) {
            user.sendMessage("commands.admin.blueprint.could-not-load");
            plugin.logError("Could not load blueprint file: " + fileName + BlueprintsManager.BLUEPRINT_SUFFIX + " " + e1.getMessage());
            return false;
        }
        user.sendMessage("general.success");
        return true;
    }

    /**
     * Save the clipboard to a file
     * @param user - user who is copying
     * @param newName - new name of this blueprint
     * @return - true if successful, false if error
     */
    public boolean save(User user, String newName, String displayName)
    {
        if (this.clipboard.isFull())
        {
            this.clipboard.getBlueprint().setName(newName);
            this.clipboard.getBlueprint().setDisplayName(displayName);

            if (this.saveBlueprint(this.clipboard.getBlueprint()))
            {
                user.sendMessage("general.success");
                return true;
            }
        }

        user.sendMessage("commands.admin.blueprint.could-not-save", "[message]", "Could not save temp blueprint file.");
        return false;
    }

    /**
     * Save a blueprint as a plain JSON file
     * @param blueprint - blueprint
     * @return true if successful, false if not
     */
    public boolean saveBlueprint(Blueprint blueprint) {
        if (blueprint.getName().isEmpty()) {
            plugin.logError("Blueprint name was empty - could not save it");
            return false;
        }
        File file = new File(blueprintFolder, blueprint.getName() + BlueprintsManager.BLUEPRINT_SUFFIX);
        String toStore = gson.toJson(blueprint, Blueprint.class);
        try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
            fileWriter.write(toStore);
        } catch (IOException e) {
            plugin.logError("Could not save blueprint file: " + file.getName());
            return false;
        }
        return true;
    }

    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(path.toFile().exists())) {
            throw new IOException("No file exists!");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(path.getParent().toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    if (!filePath.startsWith(blueprintFolder.getCanonicalPath())) {
                        throw new IOException("Entry is outside of the target directory");
                    }
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {
        // Prevent directory traversal attacks by normalizing the path
        if (!unzipFilePath.startsWith(blueprintFolder.getCanonicalFile().toPath().normalize())) {
            throw new IOException(
                    "Blueprint file is trying to write outside of the target directory! Blocked attempt to write to "
                            + unzipFilePath);
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toFile().getCanonicalPath()))) {
            byte[] bytesIn = new byte[1024];
            int read = zipInputStream.read(bytesIn);
            while (read != -1) {
                bos.write(bytesIn, 0, read);
                read = zipInputStream.read(bytesIn);
            }
        }
    }

}
