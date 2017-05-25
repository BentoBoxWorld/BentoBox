package us.tastybento.bskyblock.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 
 * Creates a connection to a database.
 * 
 */
public interface DatabaseConnecter {

    /**
     * Establishes a new connection to the database
     * 
     * @return A new connection to the database
     * @throws SQLException
     */
    public Connection createConnection() throws SQLException;

    /**
     * Returns the connection url
     * 
     * @return
     */
    public String getConnectionUrl();

    /**
     * Looks through the database (or files) and returns a known unique key
     * @param tableName 
     * @return a unique key for this record
     */
    public String getUniqueId(String tableName);

    public YamlConfiguration loadYamlFile(String string, String key);

    public void saveYamlFile(YamlConfiguration yamlFile, String tableName,
            String fileName);
}

