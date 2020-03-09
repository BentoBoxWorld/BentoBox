package world.bentobox.bentobox.database.mongodb;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

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
    private Set<Class<?>> types = new HashSet<>();

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    MongoDBDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
    }

    @Override
    public MongoDatabase createConnection(Class<?> type) {
        types.add(type);
        // Only get one client
        if (client == null) {
            MongoCredential credential = MongoCredential.createCredential(dbSettings.getUsername(),
                    dbSettings.getDatabaseName(),
                    dbSettings.getPassword().toCharArray());
            MongoClientOptions options = MongoClientOptions.builder().sslEnabled(dbSettings.isUseSSL()).build();
            client = new MongoClient(new ServerAddress(dbSettings.getHost(), dbSettings.getPort()), credential,options);
        }
        return client.getDatabase(dbSettings.getDatabaseName());
    }

    @Override
    public String getConnectionUrl() {
        return "";
    }

    @Override
    @NonNull
    public String getUniqueId(String tableName) {
        // Not used
        return "";
    }

    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        // Not used
        return false;
    }

    @Override
    public void closeConnection(Class<?> type) {
        types.remove(type);
        if (types.isEmpty() && client != null) {
            client.close();
            Bukkit.getLogger().info("Closed database connection");
        }
    }

}
