package world.bentobox.bentobox.database.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseConnector;


/**
 * Generic SQL database connector.
 */
public abstract class SQLDatabaseConnector implements DatabaseConnector
{
    /**
     * The connection url string for the sql database.
     */
    protected String connectionUrl;

    /**
     * The database connection settings.
     */
    protected final DatabaseConnectionSettingsImpl dbSettings;

    /**
     * Hikari Data Source that creates all connections.
     */
    protected static HikariDataSource dataSource;

    /**
     * Type of objects stored in database.
     */
    protected static Set<Class<?>> types = new HashSet<>();


    /**
     * Default connector constructor.
     * @param dbSettings Settings of the database.
     * @param connectionUrl Connection url for the database.
     */
    protected SQLDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings, String connectionUrl)
    {
        this.dbSettings = dbSettings;
        this.connectionUrl = connectionUrl;
    }


    /**
     * Returns connection url of database.
     * @return Database connection url.
     */
    @Override
    public String getConnectionUrl()
    {
        return connectionUrl;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getUniqueId(String tableName)
    {
        // Not used
        return "";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uniqueIdExists(String tableName, String key)
    {
        // Not used
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection(Class<?> type)
    {
        types.remove(type);

        if (types.isEmpty())
        {
            dataSource.close();
            Bukkit.getLogger().info("Closed database connection");
        }
    }


    /**
     * This method creates config that is used to create HikariDataSource.
     * @return HikariConfig object.
     */
    public abstract HikariConfig createConfig();


    /**
     * {@inheritDoc}
     */
    @Override
    public Object createConnection(Class<?> type)
    {
        types.add(type);

        // Only make one connection to the database
        if (dataSource == null)
        {
            try
            {
                dataSource = new HikariDataSource(this.createConfig());

                // Test connection
                try (Connection connection = dataSource.getConnection())
                {
                    connection.isValid(5 * 1000);
                }
            }
            catch (SQLException e)
            {
                Bukkit.getLogger().severe("Could not connect to the database! " + e.getMessage());
                dataSource = null;
            }
        }

        return dataSource;
    }
}