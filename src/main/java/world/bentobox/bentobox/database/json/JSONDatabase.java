package world.bentobox.bentobox.database.json;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

public class JSONDatabase implements DatabaseSetup {

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.DatabaseSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass) {
        return new JSONDatabaseHandler<>(BentoBox.getInstance(), dataObjectClass, new JSONDatabaseConnector(BentoBox.getInstance()));
    }
}
