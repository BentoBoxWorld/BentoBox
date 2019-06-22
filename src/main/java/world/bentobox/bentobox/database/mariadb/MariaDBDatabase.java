package world.bentobox.bentobox.database.mariadb;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @author barpec12
 * @since 1.1
 */
public class MariaDBDatabase implements DatabaseSetup {

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.DatabaseSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        return new MariaDBDatabaseHandler<>(plugin, type, new MariaDBDatabaseConnector(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDatabaseHost(),
                plugin.getSettings().getDatabasePort(),
                plugin.getSettings().getDatabaseName(),
                plugin.getSettings().getDatabaseUsername(),
                plugin.getSettings().getDatabasePassword()
                )));
    }

}
