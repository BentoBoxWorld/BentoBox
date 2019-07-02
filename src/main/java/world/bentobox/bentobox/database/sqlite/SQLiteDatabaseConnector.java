package world.bentobox.bentobox.database.sqlite;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabaseConnector implements DatabaseConnector {

    private String connectionUrl;
    private static Connection connection = null;
    private static final String DATABASE_FOLDER_NAME = "database";

    SQLiteDatabaseConnector(@NonNull BentoBox plugin) {
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        connectionUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "database.db";
    }

    @Override
    public Object createConnection() {
        // Only make one connection at a time
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(connectionUrl);
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not connect to the database! " + e.getMessage());
            }
        }
        return connection;
    }

    @Override
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not close SQLite database connection");
            }
        }
    }

    @Override
    public String getConnectionUrl() {
        return connectionUrl;
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
}
