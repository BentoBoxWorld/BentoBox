package us.tastybento.bskyblock.database.mysql;

import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class MySQLDatabase extends BSBDatabase{

    @Override
    public AbstractDatabaseHandler<?> getHandler(Plugin plugin, Class<?> type) {
        return new MySQLDatabaseHandler<>(plugin, type, new MySQLDatabaseConnecter(new DatabaseConnectionSettingsImpl()));
    }

}
