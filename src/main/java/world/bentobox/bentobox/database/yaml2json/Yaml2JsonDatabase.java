package world.bentobox.bentobox.database.yaml2json;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

public class Yaml2JsonDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new Yaml2JsonDatabaseHandler<>(BentoBox.getInstance(), type, new Yaml2JsonDatabaseConnector(BentoBox.getInstance()));
    }

}
