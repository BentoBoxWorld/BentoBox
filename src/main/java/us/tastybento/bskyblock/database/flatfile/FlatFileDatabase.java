package us.tastybento.bskyblock.database.flatfile;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.objects.Island;

public class FlatFileDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(BSkyBlock plugin, Class<?> type) {
        return new FlatFileDatabaseHandler<Island>(plugin, Island.class, new FlatFileDatabaseConnecter(plugin, null));

    }

}
