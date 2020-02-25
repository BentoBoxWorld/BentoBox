package world.bentobox.bentobox.database.sql.mariadb;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

/**
 * @author barpec12
 * @since 1.1
 */
public class MariaDBDatabaseConnector extends SQLDatabaseConnector {

    /**
     * Class for MariaDB database connections using the settings provided
     * @param dbSettings - database settings
     */
    MariaDBDatabaseConnector(DatabaseConnectionSettingsImpl dbSettings) {
        super(dbSettings, "jdbc:mysql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName()
        + "?autoReconnect=true&useSSL=" + dbSettings.isUseSSL() + "&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8");
    }

}
