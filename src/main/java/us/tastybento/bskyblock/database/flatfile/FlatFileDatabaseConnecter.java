package us.tastybento.bskyblock.database.flatfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.DatabaseConnecter;

public class FlatFileDatabaseConnecter implements DatabaseConnecter {

    private static final int MAX_LOOPS = 100;
    private static final String DATABASE_FOLDER_NAME = "database";
    private BSkyBlock plugin;
    private File dataFolder;


    public FlatFileDatabaseConnecter(BSkyBlock plugin) {
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

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.DatabaseConnecter#loadYamlFile(java.lang.String, java.lang.String)
     */
    @Override
    public YamlConfiguration loadYamlFile(String tableName, String fileName) {
        if (!fileName.endsWith(".yml")) {
            fileName = fileName + ".yml";
        }
        File yamlFile = new File(plugin.getDataFolder(), tableName + File.separator + fileName);

        YamlConfiguration config = null;
        if (yamlFile.exists()) {
            try {
                config = new YamlConfiguration();
                config.load(yamlFile);
            } catch (Exception e) {
                plugin.logError("Could not load yaml file from database " + tableName + " " + fileName + " " + e.getMessage());
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

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.DatabaseConnecter#saveYamlFile(org.bukkit.configuration.file.YamlConfiguration, java.lang.String, java.lang.String)
     */
    @Override
    public void saveYamlFile(YamlConfiguration yamlConfig, String tableName, String fileName, Map<String, String> commentMap) {
        if (!fileName.endsWith(".yml")) {
            fileName = fileName + ".yml";
        }
        File tableFolder = new File(plugin.getDataFolder(), tableName);
        File file = new File(tableFolder, fileName);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }
        try {
            yamlConfig.save(file);
        } catch (Exception e) {
            plugin.logError("Could not save yaml file to database " + tableName + " " + fileName + " " + e.getMessage());
            return;
        }
        if (commentMap != null && !commentMap.isEmpty()) {
            commentFile(file, commentMap);
        }
    }

    /**
     * Adds comments to a YAML file
     * @param file
     * @param commentMap
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
            Files.move(commentedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            plugin.logError("Could not comment config file " + file.getName() + " " + e1.getMessage());
        } 
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.DatabaseConnecter#getUniqueId(java.lang.String)
     */
    @Override
    public String getUniqueId(String tableName) {
        UUID uuid = UUID.randomUUID();
        File file = new File(dataFolder, tableName + File.separator + uuid.toString() + ".yml");
        int limit = 0;
        while (file.exists() && limit++ < MAX_LOOPS) {
            uuid = UUID.randomUUID();
            file = new File(dataFolder, tableName + File.separator + uuid.toString() + ".yml");
        }
        return uuid.toString();
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.DatabaseConnecter#uniqueIdExists(java.lang.String, java.lang.String)
     */
    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        File file = new File(dataFolder, tableName + File.separator + key + ".yml");
        return file.exists();
    }

    @Override
    public void closeConnection() {
        // Not used
    }

}
