package world.bentobox.bentobox.database.postgresql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class PostgreSQLDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass) {
        BentoBox plugin = BentoBox.getInstance();
        return new PostgreSQLDatabaseHandler<>(plugin, dataObjectClass, new PostgreSQLDatabaseConnector(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDatabaseHost(),
                plugin.getSettings().getDatabasePort(),
                plugin.getSettings().getDatabaseName(),
                plugin.getSettings().getDatabaseUsername(),
                plugin.getSettings().getDatabasePassword()
        )));
    }
}
