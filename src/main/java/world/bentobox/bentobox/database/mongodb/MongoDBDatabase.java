package world.bentobox.bentobox.database.mongodb;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

public class MongoDBDatabase implements DatabaseSetup {

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.DatabaseSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        // Check if the MongoDB plugin exists
        if (Bukkit.getServer().getPluginManager().getPlugin("BsbMongo") == null) {
            plugin.logError("You must install BsbMongo plugin for MongoDB support!");
            plugin.logError("See: https://github.com/tastybento/bsbMongo/releases/");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        return new MongoDBDatabaseHandler<>(plugin, type, new MongoDBDatabaseConnector(new DatabaseConnectionSettingsImpl(
                plugin.getSettings().getDatabaseHost(),
                plugin.getSettings().getDatabasePort(),
                plugin.getSettings().getDatabaseName(),
                plugin.getSettings().getDatabaseUsername(),
                plugin.getSettings().getDatabasePassword()
                )));
    }

}
