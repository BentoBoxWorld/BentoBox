package world.bentobox.bbox.database.mysql;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.database.AbstractDatabaseHandler;
import world.bentobox.bbox.database.BSBDbSetup;
import world.bentobox.bbox.database.DatabaseConnectionSettingsImpl;

public class MySQLDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        BentoBox plugin = BentoBox.getInstance();
        return new MySQLDatabaseHandler<>(plugin, type, new MySQLDatabaseConnecter(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
