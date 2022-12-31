package world.bentobox.bentobox.database.sql.mariadb;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @author barpec12
 * @since 1.1
 */
public class MariaDBDatabase implements DatabaseSetup
{
    /**
     * MariaDB Database Connector.
     */
    private MariaDBDatabaseConnector connector;


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type)
    {
        BentoBox plugin = BentoBox.getInstance();

        if (this.connector == null)
        {
            this.connector = new MariaDBDatabaseConnector(new DatabaseConnectionSettingsImpl(
                    plugin.getSettings().getDatabaseHost(),
                    plugin.getSettings().getDatabasePort(),
                    plugin.getSettings().getDatabaseName(),
                    plugin.getSettings().getDatabaseUsername(),
                    plugin.getSettings().getDatabasePassword(),
                    plugin.getSettings().isUseSSL(),
                    plugin.getSettings().getMaximumPoolSize()));
        }

        return new MariaDBDatabaseHandler<>(plugin, type, this.connector);
    }
}
