package world.bentobox.bentobox.database.transitiondb;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.mariadb.MariaDBDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

public class Yaml2MariaDBDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new YamlDatabase().getHandler(type), new MariaDBDatabase().getHandler(type));
    }

}
