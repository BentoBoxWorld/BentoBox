package world.bentobox.bbox.database.flatfile;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.database.AbstractDatabaseHandler;
import world.bentobox.bbox.database.BSBDbSetup;

public class FlatFileDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        return new FlatFileDatabaseHandler<>(BentoBox.getInstance(), type, new FlatFileDatabaseConnecter(BentoBox.getInstance()));
    }
    
    /**
     * Get the config
     * @param type - config object type
     * @return - the config handler
     */
    public AbstractDatabaseHandler<?> getConfig(Class<?> type) {
        return new ConfigHandler<>(BentoBox.getInstance(), type, new FlatFileDatabaseConnecter(BentoBox.getInstance()));
    }

}
