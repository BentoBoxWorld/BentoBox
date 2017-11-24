package us.tastybento.bskyblock.database.mongodb;

import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class MongoDBDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Plugin plugin, Class<?> type) {
        return null; //TODO
    }

}
