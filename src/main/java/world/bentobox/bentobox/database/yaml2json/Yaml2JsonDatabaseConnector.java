package world.bentobox.bentobox.database.yaml2json;

import java.io.File;
import java.sql.Connection;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

public class Yaml2JsonDatabaseConnector implements DatabaseConnector {

    private static final int MAX_LOOPS = 100;
    private static final String DATABASE_FOLDER_NAME = "database";
    private static final String YML = ".yml";
    private static final String JSON = ".json";
    private final BentoBox plugin;
    private final File dataFolder;

    Yaml2JsonDatabaseConnector(BentoBox plugin) {
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

        YamlConfiguration config = new YamlConfiguration();
        if (yamlFile.exists()) {
            try {
                config.load(yamlFile);
            } catch (Exception e) {
                plugin.logError("Could not load yml file from database " + tableName + " " + fileName + " " + e.getMessage());
            }
        }
        return config;
    }

    @Override
    public String getUniqueId(String tableName) {
        UUID uuid = UUID.randomUUID();
        File file = new File(dataFolder, tableName + File.separator + uuid.toString() + JSON);
        int limit = 0;
        while (file.exists() && limit++ < MAX_LOOPS) {
            uuid = UUID.randomUUID();
            file = new File(dataFolder, tableName + File.separator + uuid.toString() + JSON);
        }
        return uuid.toString();
    }

    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        File file = new File(dataFolder, tableName + File.separator + key + JSON);
        File file2 = new File(dataFolder, tableName + File.separator + key + YML);
        return file.exists() || file2.exists();
    }

    @Override
    public void closeConnection() {
        // Not used
    }

}
