package world.bentobox.bentobox.database.mongodb;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;

public class MongoDBDatabase extends DatabaseSetup {

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        // Check if the MongoDB plugin exists
        if (plugin.getServer().getPluginManager().getPlugin("BsbMongo") == null) {
            plugin.logError("You must install BsbMongo plugin for MongoDB support!");
            plugin.logError("See: https://github.com/tastybento/bsbMongo/releases/");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        return new MongoDBDatabaseHandler<>(plugin, type, new MongoDBDatabaseConnector(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
