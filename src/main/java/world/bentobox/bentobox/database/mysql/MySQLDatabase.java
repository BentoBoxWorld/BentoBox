package world.bentobox.bentobox.database.mysql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;

public class MySQLDatabase extends DatabaseSetup {


    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.BSBDbSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        return new MySQLDatabaseHandler<>(plugin, type, new MySQLDatabaseConnector(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
