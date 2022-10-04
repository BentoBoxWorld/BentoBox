package world.bentobox.bentobox.database.sql.mysql;

import com.zaxxer.hikari.HikariConfig;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

public class MySQLDatabaseConnector extends SQLDatabaseConnector
{
    /**
     * Class for MySQL database connections using the settings provided
     *
     * @param dbSettings - database settings
     */
    MySQLDatabaseConnector(@NonNull DatabaseConnectionSettingsImpl dbSettings)
    {
        super(dbSettings, String.format("jdbc:mysql://%s:%s/%s",
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
        config.setPoolName("BentoBox MySQL Pool");

        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl(this.connectionUrl);
        config.setUsername(this.dbSettings.getUsername());
        config.setPassword(this.dbSettings.getPassword());

        config.addDataSourceProperty("useSSL", this.dbSettings.isUseSSL());

        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("encoding", "UTF-8");
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("allowMultiQueries", "true");

        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");

        // Add extra properties.
        this.dbSettings.getExtraProperties().forEach(config::addDataSourceProperty);

        config.setMaximumPoolSize(this.dbSettings.getMaxConnections());

        return config;
    }
}
