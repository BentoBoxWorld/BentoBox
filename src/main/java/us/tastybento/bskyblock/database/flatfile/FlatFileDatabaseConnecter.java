package us.tastybento.bskyblock.database.flatfile;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class FlatFileDatabaseConnecter implements DatabaseConnecter {

    private static final int MAX_LOOPS = 100;
    private BSkyBlock plugin;
    private File dataFolder;


    public FlatFileDatabaseConnecter(BSkyBlock plugin, DatabaseConnectionSettingsImpl databaseConnectionSettingsImpl) {
        this.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "database");
    }

    @Override
    public Connection createConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getConnectionUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Loads a YAML file and if it does not exist it is looked for in the JAR
     * 
     * @param fileName
     * @return
     */
    @Override
    public YamlConfiguration loadYamlFile(String fileName) {
        File yamlFile = new File(dataFolder, fileName + ".yml");

        YamlConfiguration config = null;
        if (yamlFile.exists()) {
            try {
                config = new YamlConfiguration();
                config.load(yamlFile);
            } catch (Exception e) {
                e.printStackTrace();
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

    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileName
     */
    @Override
    public void saveYamlFile(YamlConfiguration yamlFile, String fileName) {
        File file = new File(dataFolder, fileName + ".yml");

        try {
            yamlFile.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public UUID getUniqueId() {
        UUID uuid = UUID.randomUUID();
        File file = new File(dataFolder, uuid.toString() + ".yml");
        int limit = 0;
        while (file.exists() && limit++ < MAX_LOOPS) {
            uuid = UUID.randomUUID();
            file = new File(dataFolder, uuid.toString() + ".yml");
        }
        return uuid;
    }

}
