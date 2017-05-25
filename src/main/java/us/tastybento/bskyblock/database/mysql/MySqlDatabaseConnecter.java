package us.tastybento.bskyblock.database.mysql;

import java.sql.Connection;
import java.sql.SQLException;

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
    public String getUniqueId(String tableName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public YamlConfiguration loadYamlFile(String string, String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveYamlFile(YamlConfiguration yamlFile, String tableName,
            String fileName) {
        // TODO Auto-generated method stub
        
    }

}
