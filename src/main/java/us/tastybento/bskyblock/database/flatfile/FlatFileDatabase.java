package us.tastybento.bskyblock.database.flatfile;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDbSetup;

public class FlatFileDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        return new FlatFileDatabaseHandler<>(BSkyBlock.getInstance(), type, new FlatFileDatabaseConnecter(BSkyBlock.getInstance()));
    }
    
    /**
     * Get the config
     * @param type - config object type
     * @return - the config handler
     */
    public AbstractDatabaseHandler<?> getConfig(Class<?> type) {
        return new ConfigHandler<>(BSkyBlock.getInstance(), type, new FlatFileDatabaseConnecter(BSkyBlock.getInstance()));
    }

}
