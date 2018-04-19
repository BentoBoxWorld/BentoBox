package us.tastybento.bskyblock.database.mongodb;

import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MongoDBDatabaseConnecter implements DatabaseConnecter {

    private MongoClient client;
    private MongoCredential credential;
    private DatabaseConnectionSettingsImpl dbSettings;

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings
     */
    public MongoDBDatabaseConnecter(DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
        credential = MongoCredential.createCredential(dbSettings.getUsername(),
                dbSettings.getDatabaseName(),
                dbSettings.getPassword().toCharArray());
        MongoClientOptions options = MongoClientOptions.builder().sslEnabled(false).build();
        client = new MongoClient(new ServerAddress(dbSettings.getHost(), dbSettings.getPort()),credential,options);
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
