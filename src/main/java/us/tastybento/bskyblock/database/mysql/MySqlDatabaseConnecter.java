package us.tastybento.bskyblock.database.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.DatabaseConnectionSettingsImpl;

public class MySqlDatabaseConnecter implements DatabaseConnecter {

    public MySqlDatabaseConnecter(
            DatabaseConnectionSettingsImpl databaseConnectionSettingsImpl) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public Connection createConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getConnectionUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public YamlConfiguration loadYamlFile(String simpleName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveYamlFile(YamlConfiguration config, String simpleName) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public UUID getUniqueId() {
        // TODO Auto-generated method stub
        return null;
    }

}
