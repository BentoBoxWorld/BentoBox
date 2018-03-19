package us.tastybento.bskyblock.database.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MySQLDatabaseConnecter implements DatabaseConnecter {

    private String connectionUrl;
    private DatabaseConnectionSettingsImpl dbSettings;
    private Connection connection = null;

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings
     */
    public MySQLDatabaseConnecter(DatabaseConnectionSettingsImpl dbSettings) {
        this.dbSettings = dbSettings;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Could not instantiate JDBC driver! " + e.getMessage());
        }
        // jdbc:mysql://localhost:3306/Peoples?autoReconnect=true&useSSL=false
        connectionUrl = "jdbc:mysql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName() + "?autoReconnect=true&useSSL=false&allowMultiQueries=true";
    }

    @Override
    public Connection createConnection() {
        try {
            connection = DriverManager.getConnection(connectionUrl, dbSettings.getUsername(), dbSettings.getPassword());
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Could not connect to the database! " + e.getMessage());
        }
        return connection;
    }

    @Override
    public String getConnectionUrl() {
        return connectionUrl;
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
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not close MySQL database connection");
            }
        }
    }
}
