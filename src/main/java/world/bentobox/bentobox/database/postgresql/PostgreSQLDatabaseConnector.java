package world.bentobox.bentobox.database.postgresql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseConnector;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class PostgreSQLDatabaseConnector implements DatabaseConnector {

    private String connectionUrl;
    private DatabaseConnectionSettingsImpl dbSettings;
    private Connection connection = null;
    private Set<Class<?>> types = new HashSet<>();

    /**
     * Class for PostgreSQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    PostgreSQLDatabaseConnector(@NonNull DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
        connectionUrl = "jdbc:postgresql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName()
        + "?autoReconnect=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8";
    }

    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public @NonNull String getUniqueId(String tableName) {
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
                Bukkit.getLogger().severe("Could not close PostgreSQL database connection");
            }
        }
    }

    @Override
    public Object createConnection(Class<?> type) {
        types.add(type);
        // Only make one connection to the database
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(connectionUrl, dbSettings.getUsername(), dbSettings.getPassword());
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not connect to the database! " + e.getMessage());
            }
        }
        return connection;
    }
}
