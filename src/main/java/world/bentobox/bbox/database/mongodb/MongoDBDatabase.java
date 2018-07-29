package world.bentobox.bbox.database.mongodb;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.database.AbstractDatabaseHandler;
import world.bentobox.bbox.database.BSBDbSetup;
import world.bentobox.bbox.database.DatabaseConnectionSettingsImpl;

public class MongoDBDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        BentoBox plugin = BentoBox.getInstance();
        // Check if the MongoDB plugin exists
        if (plugin.getServer().getPluginManager().getPlugin("BsbMongo") == null) {
            plugin.logError("You must install BsbMongo plugin for MongoDB support!");
            plugin.logError("See: https://github.com/tastybento/bsbMongo/releases/");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        return new MongoDBDatabaseHandler<>(plugin, type, new MongoDBDatabaseConnecter(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDbHost(),
                plugin.getSettings().getDbPort(),
                plugin.getSettings().getDbName(),
                plugin.getSettings().getDbUsername(),
                plugin.getSettings().getDbPassword()
                )));
    }

}
