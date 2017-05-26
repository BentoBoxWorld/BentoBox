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

    /**
     * Loads a YAML file. Used by the flat file database
     * @param string
     * @param key
     * @return Yaml Configuration
     */
    public YamlConfiguration loadYamlFile(String string, String key);

    /**
     * Save the Yaml Config
     * @param yamlFile
     * @param tableName - analogous to a table in a database
     * @param fileName - the name of the record. Must be unique.
     */
    public void saveYamlFile(YamlConfiguration yamlFile, String tableName,
            String fileName);
}

