package us.tastybento.bskyblock.database.flatfile;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class FlatFileDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        return new FlatFileDatabaseHandler<>(BSkyBlock.getInstance(), type, new FlatFileDatabaseConnecter(BSkyBlock.getInstance(), null));
    }

}
