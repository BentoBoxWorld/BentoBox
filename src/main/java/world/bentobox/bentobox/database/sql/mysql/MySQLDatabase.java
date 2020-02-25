package world.bentobox.bentobox.database.sql.mysql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

public class MySQLDatabase implements DatabaseSetup {

    private MySQLDatabaseConnector connector;

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.DatabaseSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        if (connector == null) {
            connector = new MySQLDatabaseConnector(new DatabaseConnectionSettingsImpl(
                    plugin.getSettings().getDatabaseHost(),
                    plugin.getSettings().getDatabasePort(),
                    plugin.getSettings().getDatabaseName(),
                    plugin.getSettings().getDatabaseUsername(),
                    plugin.getSettings().getDatabasePassword(),
                    plugin.getSettings().isUseSSL()
                    ));
        }
        return new MySQLDatabaseHandler<>(plugin, type, connector);
    }

}
