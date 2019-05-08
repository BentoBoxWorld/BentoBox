package world.bentobox.bentobox.database.transitiondb;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

public class Json2YamlDatabase implements DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        return new TransitionDatabaseHandler<>(type, new JSONDatabase().getHandler(type), new YamlDatabase().getHandler(type));
    }

}
