package world.bentobox.bentobox.database.mongodb;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.DatabaseSetup;

public class MongoDBDatabase implements DatabaseSetup {

    private MongoDBDatabaseConnector connector;

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.DatabaseSetup#getHandler(java.lang.Class)
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> type) {
        BentoBox plugin = BentoBox.getInstance();
        // Check if the MongoDB plugin exists
        if (Bukkit.getPluginManager().getPlugin("BsbMongo") == null) {
            plugin.logError("You must install BsbMongo plugin for MongoDB support!");
            plugin.logError("See: https://github.com/tastybento/bsbMongo/releases/");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return null;
        }
        if (connector == null) {
            connector = new MongoDBDatabaseConnector(new DatabaseConnectionSettingsImpl(
                    plugin.getSettings().getDatabaseHost(),
                    plugin.getSettings().getDatabasePort(),
                    plugin.getSettings().getDatabaseName(),
                    plugin.getSettings().getDatabaseUsername(),
                    plugin.getSettings().getDatabasePassword(),
                    plugin.getSettings().isUseSSL()
            ), plugin.getSettings().getMongodbConnectionUri());
        }
        return new MongoDBDatabaseHandler<>(plugin, type, connector);
    }

}
