package world.bentobox.bentobox.database.transition;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.sql.sqlite.SQLiteDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;


/**
 * @author BONNe
 * @since 1.6.0
 */
public class Yaml2SQLiteDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new YamlDatabase().getHandler(type), new SQLiteDatabase().getHandler(type));
    }

}
