package world.bentobox.bentobox.database.json;

import java.io.File;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

public class JSONDatabaseConnector implements DatabaseConnector {

    private static final int MAX_LOOPS = 100;
    private static final String DATABASE_FOLDER_NAME = "database";
    private static final String JSON = ".json";
    private final File dataFolder;

    JSONDatabaseConnector(BentoBox plugin) {
        dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
    }

    @Override
    @NonNull
    public String getUniqueId(String tableName) {
        UUID uuid = UUID.randomUUID();
        File file = new File(dataFolder, tableName + File.separator + uuid + JSON);
        int limit = 0;
        while (file.exists() && limit++ < MAX_LOOPS) {
            uuid = UUID.randomUUID();
            file = new File(dataFolder, tableName + File.separator + uuid + JSON);
        }
        return uuid.toString();
    }

    @Override
    public boolean uniqueIdExists(String tableName, String key) {
        File file = new File(dataFolder, tableName + File.separator + key + JSON);
        return file.exists();
    }

    @Override
    public String getConnectionUrl() {
        return null; // Not used
    }

    @Override
    public Object createConnection(Class<?> type) {
        // Not used
        return null;
    }

    @Override
    public void closeConnection(Class<?> type) {
        // Not used
    }

}
