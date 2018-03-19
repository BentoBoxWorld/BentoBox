package us.tastybento.bskyblock.database.mongodb;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MongoDBDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        BSkyBlock plugin = BSkyBlock.getInstance();
        return new MongoDBDatabaseHandler<>(plugin, type, new MongoDBDatabaseConnecter(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
