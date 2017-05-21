package us.tastybento.bskyblock.database.flatfile;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class FlatFileDatabaseConnecter implements DatabaseConnecter {

    private BSkyBlock plugin;

    public FlatFileDatabaseConnecter(BSkyBlock plugin, DatabaseConnectionSettingsImpl databaseConnectionSettingsImpl) {
        this.plugin = plugin;
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
        File dataFolder = new File(plugin.getDataFolder(), "database");
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
        File dataFolder = new File(plugin.getDataFolder(), "database");
        File file = new File(dataFolder, fileName + ".yml");

        try {
            yamlFile.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
