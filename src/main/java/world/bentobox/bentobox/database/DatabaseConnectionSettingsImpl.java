package world.bentobox.bentobox.database;

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
     * Hosts database settings
     * @param host - database host
     * @param port - port
     * @param databaseName - database name
     * @param username - username 
     * @param password - password
     */
    public DatabaseConnectionSettingsImpl(String host, int port, String databaseName, String username, String password, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
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
}
