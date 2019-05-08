package world.bentobox.bentobox.database.transitiondb;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

public class MySQL2YamlDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new MySQLDatabase().getHandler(type), new YamlDatabase().getHandler(type));
    }

}
