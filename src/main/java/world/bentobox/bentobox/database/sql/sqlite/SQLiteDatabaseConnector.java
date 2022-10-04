package world.bentobox.bentobox.database.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;

import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabaseConnector extends SQLDatabaseConnector
{
    /**
     * Default constructor.
     */
    SQLiteDatabaseConnector(String connectionUrl)
    {
        super(null, connectionUrl);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public HikariConfig createConfig()
    {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        config.setPoolName("BentoBox SQLite Pool");
        config.addDataSourceProperty("encoding", "UTF-8");
        config.addDataSourceProperty("url", this.connectionUrl);
        config.setMaximumPoolSize(100);

        return config;
    }
}
