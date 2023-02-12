package world.bentobox.bentobox.database.sql.postgresql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class PostgreSQLDatabase implements DatabaseSetup
{
    /**
     * PostgreSQL Database Connector.
     */
    PostgreSQLDatabaseConnector connector;


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass)
    {
        BentoBox plugin = BentoBox.getInstance();

        if (this.connector == null)
        {
            this.connector = new PostgreSQLDatabaseConnector(new DatabaseConnectionSettingsImpl(
                    plugin.getSettings().getDatabaseHost(),
                    plugin.getSettings().getDatabasePort(),
                    plugin.getSettings().getDatabaseName(),
                    plugin.getSettings().getDatabaseUsername(),
                    plugin.getSettings().getDatabasePassword(),
                    plugin.getSettings().isUseSSL(),
                    plugin.getSettings().getMaximumPoolSize()));
        }

        return new PostgreSQLDatabaseHandler<>(plugin, dataObjectClass, this.connector);
    }
}
