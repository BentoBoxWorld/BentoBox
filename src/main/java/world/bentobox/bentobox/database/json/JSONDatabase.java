package world.bentobox.bentobox.database.json;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

public class JSONDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass) {
        return new JSONDatabaseHandler<>(BentoBox.getInstance(), dataObjectClass, new JSONDatabaseConnector(BentoBox.getInstance()));
    }
}
