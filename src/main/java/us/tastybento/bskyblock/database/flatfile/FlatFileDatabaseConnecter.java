package us.tastybento.bskyblock.database.flatfile;

import java.io.File;
import java.sql.Connection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class FlatFileDatabaseConnecter implements DatabaseConnecter {

    private static final int MAX_LOOPS = 100;
    private static final String DATABASE_FOLDER_NAME = "database";
    private Plugin plugin;
    private File dataFolder;


    public FlatFileDatabaseConnecter(Plugin plugin, DatabaseConnectionSettingsImpl databaseConnectionSettingsImpl) {
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
                Bukkit.getLogger().severe("Could not load yaml file from database " + tableName + " " + fileName + " " + e.getMessage());
            }
        } else {
            // Create the missing file
            config = new YamlConfiguration();
            plugin.getLogger().info("No " + fileName + " found. Creating it...");
            try {
                if (plugin.getResource(fileName) != null) {
                    plugin.getLogger().info("Using default found in jar file.");
                    plugin.saveResource(fileName, false);
                    config = new YamlConfiguration();
                    config.load(yamlFile);
                } else {
                    config.save(yamlFile);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create the " + fileName + " file!");
            }
        }
        return config;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.DatabaseConnecter#saveYamlFile(org.bukkit.configuration.file.YamlConfiguration, java.lang.String, java.lang.String)
     */
    @Override
    public void saveYamlFile(YamlConfiguration yamlConfig, String tableName, String fileName) {
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
            Bukkit.getLogger().severe("Could not save yaml file to database " + tableName + " " + fileName + " " + e.getMessage());
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

}
