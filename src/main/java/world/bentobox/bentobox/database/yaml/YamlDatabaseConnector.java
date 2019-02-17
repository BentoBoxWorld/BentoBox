package world.bentobox.bentobox.database.yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Charsets;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

public class YamlDatabaseConnector implements DatabaseConnector {

    private static final int MAX_LOOPS = 100;
    private static final String DATABASE_FOLDER_NAME = "database";
    private static final String YML = ".yml";
    private final BentoBox plugin;
    private final File dataFolder;

    YamlDatabaseConnector(BentoBox plugin) {
        this.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
    }

    @Override
    public Connection createConnection() {
        return null; // Not used
    }

    @Override
    public String getConnectionUrl() {
        return null; // Not used
    }

    public YamlConfiguration loadYamlFile(String tableName, String fileName) {
        if (!fileName.endsWith(YML)) {
            fileName = fileName + YML;
        }
        File yamlFile = new File(plugin.getDataFolder(), tableName + File.separator + fileName);

        YamlConfiguration config = null;
        if (yamlFile.exists()) {
            try {
                config = new YamlConfiguration();
                config.load(yamlFile);
            } catch (Exception e) {
                /*
                 * TODO: Temporary fix for removing UUID tags. Remove in a few versions
                 */
                if (e.getMessage().contains("!!java.util.UUID")) {
                    removeStringFromFile(yamlFile);
                    // Try again
                    try {
                        Objects.requireNonNull(config).load(yamlFile);
                    } catch (IOException | InvalidConfigurationException e1) {
                        plugin.logError("Could not load yml file from database " + tableName + " " + fileName + " " + e.getMessage());
                    }
                } else {
                    plugin.logError("Could not load yml file from database " + tableName + " " + fileName + " " + e.getMessage());
                }
            }
        } else {
            // Create the missing file
            config = new YamlConfiguration();
            plugin.log("No " + fileName + " found. Creating it...");
            try {
                if (plugin.getResource(fileName) != null) {
                    plugin.log("Using default found in jar file.");
                    plugin.saveResource(fileName, false);
                    config = new YamlConfiguration();
                    config.load(yamlFile);
                } else {
                    config.save(yamlFile);
                }
            } catch (Exception e) {
                plugin.logError("Could not create the " + fileName + " file!");
            }
        }
        return config;
    }

    private void removeStringFromFile(File yamlFile) {
        PrintWriter writer = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(yamlFile), StandardCharsets.UTF_8))){
            File temp = File.createTempFile("file", ".tmp", yamlFile.getParentFile());
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null;) {
                line = line.replace("!!java.util.UUID", "");
                writer.println(line);
            }
            if (yamlFile.delete() && !temp.renameTo(yamlFile)) {
                plugin.logError("Could not rename fixed Island object. Are the writing permissions correctly setup?");
            }
        } catch (Exception e) {
            plugin.logError("Could not fix Island object - skipping - " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void saveYamlFile(String data, String tableName, String fileName, Map<String, String> commentMap) {
        String name = fileName.endsWith(YML) ? fileName : fileName + YML;
        File tableFolder = new File(plugin.getDataFolder(), tableName);
        File file = new File(tableFolder, name);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            writer.write(data);
        } catch (IOException e) {
            plugin.logError("Could not save yml file: " + tableName + " " + fileName + " " + e.getMessage());
            return;
        }
        if (commentMap != null && !commentMap.isEmpty()) {
            commentFile(new File(tableFolder, name), commentMap);
        }
    }

    /**
     * Adds comments to a YAML file
     * @param file - file
     * @param commentMap - map of comments to apply to file
     */
    private void commentFile(File file, Map<String, String> commentMap) {
        // Run through the file and add in the comments
        File commentedFile = new File(file.getPath() + ".tmp");
        List<String> newFile = new ArrayList<>();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                // See if there are any comments in this line
                for (Entry<String, String> e : commentMap.entrySet()) {
                    if (nextLine.contains(e.getKey())) {
                        // We want the comment to start at the same level as the entry
                        StringBuilder commentLine = new StringBuilder();
                        for (int i = 0; i < nextLine.indexOf(e.getKey()); i++){
                            commentLine.append(' ');
                        }
                        commentLine.append(e.getValue());
                        nextLine = commentLine.toString();
                        break;
                    }
                }
                newFile.add(nextLine);
            }
            Files.write(commentedFile.toPath(), (Iterable<String>)newFile.stream()::iterator);
            copyFileUsingStream(commentedFile, file);
            Files.delete(commentedFile.toPath());
        } catch (IOException e1) {
            plugin.logError("Could not comment config file " + file.getName() + " " + e1.getMessage());
        }
    }

    /**
     * This method is necessary because Windows has problems with Files.copy and file locking.
     * @param source - file
     * @param dest - file
     * @throws IOException - exception
     */
    private void copyFileUsingStream(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    @Override
    public String getUniqueId(String tableName) {
        UUID uuid = UUID.randomUUID();
        File file = new File(dataFolder, tableName + File.separator + uuid.toString() + YML);
        int limit = 0;
        while (file.exists() && limit++ < MAX_LOOPS) {
            uuid = UUID.randomUUID();
            file = new File(dataFolder, tableName + File.separator + uuid.toString() + YML);
        }
        return uuid.toString();
    }

    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        File file = new File(dataFolder, tableName + File.separator + key + YML);
        return file.exists();
    }

    @Override
    public void closeConnection() {
        // Not used
    }

}
