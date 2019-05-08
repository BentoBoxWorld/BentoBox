package world.bentobox.bentobox.database.transitiondb;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class Json2MySQLDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new JSONDatabase().getHandler(type), new MySQLDatabase().getHandler(type));
    }

}
