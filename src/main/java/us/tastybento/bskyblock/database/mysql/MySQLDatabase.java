package us.tastybento.bskyblock.database.mysql;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDbSetup;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MySQLDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        BSkyBlock plugin = BSkyBlock.getInstance();
        return new MySQLDatabaseHandler<>(plugin, type, new MySQLDatabaseConnecter(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
