package us.tastybento.bskyblock.database.mysql;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class MySQLDatabase extends BSBDatabase{

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
