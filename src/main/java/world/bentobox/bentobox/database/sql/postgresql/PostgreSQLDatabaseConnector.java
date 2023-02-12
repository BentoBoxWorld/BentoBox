package world.bentobox.bentobox.database.sql.postgresql;

import org.eclipse.jdt.annotation.NonNull;

import com.zaxxer.hikari.HikariConfig;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;


/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class PostgreSQLDatabaseConnector extends SQLDatabaseConnector
{
    /**
     * Class for PostgreSQL database connections using the settings provided
     *
     * @param dbSettings - database settings
     */
    PostgreSQLDatabaseConnector(@NonNull DatabaseConnectionSettingsImpl dbSettings)
    {
        // connectionUrl is not used in PostgreSQL connection.
        super(dbSettings, "");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public HikariConfig createConfig()
    {
        HikariConfig config = new HikariConfig();
        config.setPoolName("BentoBox PostgreSQL Pool");

        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.addDataSourceProperty("user", this.dbSettings.getUsername());
        config.addDataSourceProperty("password", this.dbSettings.getPassword());
        config.addDataSourceProperty("databaseName", this.dbSettings.getDatabaseName());
        config.addDataSourceProperty("serverName", this.dbSettings.getHost());
        config.addDataSourceProperty("portNumber", this.dbSettings.getPort());

        config.addDataSourceProperty("ssl", this.dbSettings.isUseSSL());

        // Add extra properties.
        this.dbSettings.getExtraProperties().forEach(config::addDataSourceProperty);

        config.setMaximumPoolSize(this.dbSettings.getMaxConnections());

        return config;
    }
}
