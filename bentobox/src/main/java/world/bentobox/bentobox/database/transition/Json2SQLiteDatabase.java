package world.bentobox.bentobox.database.transition;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.sql.sqlite.SQLiteDatabase;


/**
 * @author BONNe
 * @since 1.6.0
 */
public class Json2SQLiteDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new JSONDatabase().getHandler(type), new SQLiteDatabase().getHandler(type));
    }

}
