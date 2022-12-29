package world.bentobox.bentobox.database.sql.mariadb;

import org.eclipse.jdt.annotation.NonNull;

import com.zaxxer.hikari.HikariConfig;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

/**
 * @author barpec12
 * @since 1.1
 */
public class MariaDBDatabaseConnector extends SQLDatabaseConnector
{
    /**
     * Class for MariaDB database connections using the settings provided
     * @param dbSettings - database settings
     */
    MariaDBDatabaseConnector(@NonNull DatabaseConnectionSettingsImpl dbSettings)
    {
        // MariaDB does not use connectionUrl.
        super(dbSettings, String.format("jdbc:mariadb://%s:%s/%s",
            dbSettings.getHost(),
            dbSettings.getPort(),
            dbSettings.getDatabaseName()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public HikariConfig createConfig()
    {
        HikariConfig config = new HikariConfig();

        config.setPoolName("BentoBox MariaDB Pool");
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        config.setJdbcUrl(this.connectionUrl);
        config.addDataSourceProperty("user", this.dbSettings.getUsername());
        config.addDataSourceProperty("password", this.dbSettings.getPassword());

        config.addDataSourceProperty("useSsl", this.dbSettings.isUseSSL());
        config.addDataSourceProperty("allowMultiQueries", "true");

        // Add extra properties.
        this.dbSettings.getExtraProperties().forEach(config::addDataSourceProperty);

        config.setMaximumPoolSize(this.dbSettings.getMaxConnections());

        return config;
    }
}
