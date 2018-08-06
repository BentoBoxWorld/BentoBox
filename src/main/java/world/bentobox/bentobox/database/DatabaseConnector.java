package world.bentobox.bentobox.database;

import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * Creates a connection to a database.
 *
 */
public interface DatabaseConnector {

    /**
     * Establishes a new connection to the database
     *
     * @return A new connection to the database using the settings provided
     */
     Object createConnection();
     
     /**
     * Close the database connection
     */
    void closeConnection();
    
    /**
     * Returns the connection url
     *
     * @return the connector's URL
     */
     String getConnectionUrl();

    /**
     * Looks through the database (or files) and returns a known unique key
     * @param tableName - name of the table
     * @return a unique key for this record
     */
     String getUniqueId(String tableName);

    /**
     * Check if a key exists in the database in this table or not
     * @param tableName - name of the table
     * @param key - key to check
     * @return true if it exists
     */
     boolean uniqueIdExists(String tableName, String key);

    /**
     * Loads a YAML file. Used by the flat file database
     * @param tableName - the table name to load
     * @param fileName - the filename
     * @return Yaml Configuration
     */
     YamlConfiguration loadYamlFile(String tableName, String fileName);

    /**
     * Save the Yaml Config
     * @param yamlConfig - the YAML config
     * @param path - analogous to a table name in a database
     * @param fileName - the name of the record. Must be unique.
     * @param commentMap - map of comments, may be empty
     */
    void saveYamlFile(YamlConfiguration yamlConfig, String path, String fileName, Map<String, String> commentMap);

}

