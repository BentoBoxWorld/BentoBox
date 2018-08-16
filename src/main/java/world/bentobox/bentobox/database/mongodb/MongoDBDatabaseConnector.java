package world.bentobox.bentobox.database.mongodb;

import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseConnector;

public class MongoDBDatabaseConnector implements DatabaseConnector {

    private MongoClient client;
    private DatabaseConnectionSettingsImpl dbSettings;

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    public MongoDBDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
        MongoCredential credential = MongoCredential.createCredential(dbSettings.getUsername(),
                dbSettings.getDatabaseName(),
                dbSettings.getPassword().toCharArray());
        MongoClientOptions options = MongoClientOptions.builder().sslEnabled(false).build();
        client = new MongoClient(new ServerAddress(dbSettings.getHost(), dbSettings.getPort()), credential,options);
    }

    @Override
    public MongoDatabase createConnection() {
        return client.getDatabase(dbSettings.getDatabaseName());
    }

    @Override
    public String getConnectionUrl() {
        return "";
    }

    @Override
    public String getUniqueId(String tableName) {
        // Not used
        return "";
    }

    @Override
    public YamlConfiguration loadYamlFile(String string, String key) {
        // Not used
        return null;
    }

    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        // Not used
        return false;
    }

    @Override
    public void saveYamlFile(YamlConfiguration yamlConfig, String tableName, String fileName,
            Map<String, String> commentMap) {
        // Not used
        
    }

    @Override
    public void closeConnection() {
        client.close();
        
    }

}
