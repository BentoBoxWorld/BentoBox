package world.bentobox.bentobox.database.transition;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.sql.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class Yaml2MySQLDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new YamlDatabase().getHandler(type), new MySQLDatabase().getHandler(type));
    }

}
