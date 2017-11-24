package us.tastybento.bskyblock.database.sqlite;

import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class SQLiteDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Plugin plugin, Class<?> type) {
        // return new SQLLiteDatabaseHandler<Island>(plugin, Island.class, new FlatFileDatabaseConnecter(plugin, null));
        return null;
    }

}
