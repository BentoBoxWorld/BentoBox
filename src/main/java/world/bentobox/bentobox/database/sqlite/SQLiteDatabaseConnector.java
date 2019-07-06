package world.bentobox.bentobox.database.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabaseConnector implements DatabaseConnector {

    private String connectionUrl;
    private Connection connection = null;
    private static final String DATABASE_FOLDER_NAME = "database";
    private Set<Class<?>> types = new HashSet<>();

    SQLiteDatabaseConnector(@NonNull BentoBox plugin) {
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                BentoBox.getInstance().logError("Could not create database folder!");
            }
        }
        connectionUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "database.db";
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

    @Override
    public void closeConnection(Class<?> type) {
        types.remove(type);
        if (types.isEmpty() && connection != null) {
            try {
                connection.close();
                Bukkit.getLogger().info("Closed database connection");
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not close SQLite database connection");
            }
        }
    }

    @Override
    public Object createConnection(Class<?> type) {
        types.add(type);
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
}
