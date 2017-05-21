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
     * @param simpleName
     * @return
     */
    public YamlConfiguration loadYamlFile(String simpleName);

    /**
     * @param config
     * @param simpleName
     */
    public void saveYamlFile(YamlConfiguration config, String simpleName);
}

