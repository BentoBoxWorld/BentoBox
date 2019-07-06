package world.bentobox.bentobox.database.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseConnector;

public class MySQLDatabaseConnector implements DatabaseConnector {

    private String connectionUrl;
    private DatabaseConnectionSettingsImpl dbSettings;
    private Connection connection = null;
    private Set<Class<?>> types = new HashSet<>();

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    MySQLDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
        connectionUrl = "jdbc:mysql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName()
        + "?autoReconnect=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8";
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
                Bukkit.getLogger().severe("Could not close MySQL database connection");
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
