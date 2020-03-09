package world.bentobox.bentobox.database.sql.mysql;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

public class MySQLDatabaseConnector extends SQLDatabaseConnector {

    /**
     * Class for MySQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    MySQLDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings) {
        super(dbSettings, "jdbc:mysql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName()
        + "?autoReconnect=true&useSSL=" + dbSettings.isUseSSL() + "&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8");
    }
}
