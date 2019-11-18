package world.bentobox.bentobox.database.transition;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.sql.postgresql.PostgreSQLDatabase;

/**
 * @author Poslovitch
 * @since 1.6.0
 */
public class Json2PostgreSQLDatabase implements DatabaseSetup {
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass) {
        return new TransitionDatabaseHandler<>(dataObjectClass, new JSONDatabase().getHandler(dataObjectClass), new PostgreSQLDatabase().getHandler(dataObjectClass));
    }
}
