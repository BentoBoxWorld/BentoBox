package us.tastybento.bskyblock.database.mysql;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class MySQLDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(BSkyBlock plugin, Class<?> type) {
        return new MySQLDatabaseHandler<>(plugin, type, new MySQLDatabaseConnecter(new DatabaseConnectionSettingsImpl()));
    }

}
