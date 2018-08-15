package world.bentobox.bentobox.database.flatfile;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

public class FlatFileDatabase implements DatabaseSetup {

    /**
     * Get the config
     * @param <T> - Class type
     * @param type - config object type
     * @return - the config handler
     */
    public <T> AbstractDatabaseHandler<T> getConfig(Class<T> type) {
        return new ConfigHandler<>(BentoBox.getInstance(), type, new FlatFileDatabaseConnector(BentoBox.getInstance()));
    }

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new FlatFileDatabaseHandler<>(BentoBox.getInstance(), type, new FlatFileDatabaseConnector(BentoBox.getInstance()));
    }

}
