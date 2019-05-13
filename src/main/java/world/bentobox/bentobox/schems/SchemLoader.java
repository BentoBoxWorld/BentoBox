package world.bentobox.bentobox.schems;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bentobox.BentoBox;

/**
 * This class loads schems so they can be converted to blueprints
 * @author tastybento
 * @since 1.5.0
 */
public class SchemLoader {

    private static final String LOAD_ERROR = "Could not load schems file - does not exist : ";

    private YamlConfiguration blockConfig;

    private BentoBox plugin;

    private File schemFolder;

    public SchemLoader(BentoBox plugin, File schemFolder) {
        this.plugin = plugin;
        this.schemFolder = schemFolder;
        blockConfig = new YamlConfiguration();
    }

    /**
     * @return the blockConfig
     */
    public YamlConfiguration getBlockConfig() {
        return blockConfig;
    }

    /**
     * Load a file to clipboard
     * @param fileName - filename in schems folder
     * @throws FileNotFoundException - if a file cannot be found
     * @throws IOException - if there's a load error with unziping or name
     * @throws InvalidConfigurationException - the YAML of the schem is at fault
     */
    public void load(String fileName) throws FileNotFoundException, IOException, InvalidConfigurationException {
        File zipFile = new File(schemFolder, fileName + ".schem");
        if (!zipFile.exists()) {
            plugin.logError(LOAD_ERROR + zipFile.getName());
            throw new FileNotFoundException(LOAD_ERROR + zipFile.getName());
        }
        unzip(zipFile.getAbsolutePath());
        File file = new File(schemFolder, fileName);
        if (!file.exists()) {
            plugin.logError(LOAD_ERROR + file.getName());
            throw new FileNotFoundException(LOAD_ERROR + file.getName());
        }
        blockConfig.load(file);
        Files.delete(file.toPath());
    }

    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(path.toFile().exists())) {
            throw new FileNotFoundException("No file exists to unzip!");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(path.getParent().toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

}
