package world.bentobox.bentobox.database;


import java.util.Collections;
import java.util.Map;


/**
 * The type Database connection settings.
 */
public class DatabaseConnectionSettingsImpl {
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    /**
     * Allows to enable SSL protection to databases that supports it, like mariaDB, MySQL,
     * PostgreSQL and MongoDB.
     * @since 1.12.0
     */
    private boolean useSSL;

    /**
     * Number of max connections in pool.
     * @since 1.21.0
     */
    private int maxConnections;

    /**
     * Map of extra properties.
     * @since 1.21.0
     */
    private Map<String, String> extraProperties;

    /**
     * Hosts database settings
     * @param host - database host
     * @param port - port
     * @param databaseName - database name
     * @param username - username 
     * @param password - password
     * @param extraProperties Map with extra properties.
     */
    public DatabaseConnectionSettingsImpl(String host,
        int port,
        String databaseName,
        String username,
        String password,
        boolean useSSL,
        int maxConnections,
        Map<String, String> extraProperties)
    {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.maxConnections = maxConnections;
        this.extraProperties = extraProperties;
    }


    /**
     * Hosts database settings
     * @param host - database host
     * @param port - port
     * @param databaseName - database name
     * @param username - username
     * @param password - password
     * @param useSSL - ssl usage.
     * @param maxConnections - number of maximal connections in pool.
     */
    public DatabaseConnectionSettingsImpl(String host,
        int port,
        String databaseName,
        String username,
        String password,
        boolean useSSL,
        int maxConnections)
    {
        this(host, port, databaseName, username, password, useSSL, maxConnections, Collections.emptyMap());
    }


    /**
     * Hosts database settings
     * @param host - database host
     * @param port - port
     * @param databaseName - database name
     * @param username - username
     * @param password - password
     * @param useSSL - ssl usage.
     */
    public DatabaseConnectionSettingsImpl(String host,
        int port,
        String databaseName,
        String username,
        String password,
        boolean useSSL)
    {
        this(host, port, databaseName, username, password, useSSL, 0, Collections.emptyMap());
    }


    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @param databaseName the databaseName to set
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This method returns the ssl value.
     * @return the value of ssl.
     */
    public boolean isUseSSL() {
        return useSSL;
    }

    /**
     * This method sets the ssl value.
     * @param useSSL the ssl new value.
     *
     */
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }


    /**
     * Gets max connections.
     *
     * @return the max connections
     */
    public int getMaxConnections()
    {
        return this.maxConnections;
    }


    /**
     * Sets max connections.
     *
     * @param maxConnections the max connections
     */
    public void setMaxConnections(int maxConnections)
    {
        this.maxConnections = maxConnections;
    }


    /**
     * Gets extra properties.
     *
     * @return the extra properties
     */
    public Map<String, String> getExtraProperties()
    {
        return extraProperties;
    }


    /**
     * Sets extra properties.
     *
     * @param extraProperties the extra properties
     */
    public void setExtraProperties(Map<String, String> extraProperties)
    {
        this.extraProperties = extraProperties;
    }
}
