package us.tastybento.bskyblock.database.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        connectionUrl = "jdbc:mysql://" + dbSettings.getHost() + "/" + dbSettings.getDatabaseName() + "?autoReconnect=true&useSSL=false&allowMultiQueries=true";
    }

    @Override
    public Connection createConnection() throws SQLException {
        connection = DriverManager.getConnection(connectionUrl, dbSettings.getUsername(), dbSettings.getPassword());
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
    public void saveYamlFile(YamlConfiguration yamlFile, String tableName, String fileName) {
        // Not used

    }

    @Override
    public boolean uniqueIdExists(String simpleName, String key) {
        // Not used
        return false;
    }

}
