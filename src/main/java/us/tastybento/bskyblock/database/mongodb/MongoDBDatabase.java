package us.tastybento.bskyblock.database.mongodb;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDbSetup;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MongoDBDatabase extends BSBDbSetup{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Class<?> type) {
        BSkyBlock plugin = BSkyBlock.getInstance();
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
