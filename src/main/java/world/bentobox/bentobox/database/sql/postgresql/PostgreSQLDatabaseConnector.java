package world.bentobox.bentobox.database.sql.postgresql;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class PostgreSQLDatabaseConnector extends SQLDatabaseConnector {

    /**
     * Class for PostgreSQL database connections using the settings provided
     * @param dbSettings - database settings
     */
    PostgreSQLDatabaseConnector(@NonNull DatabaseConnectionSettingsImpl dbSettings) {
        super(dbSettings, "jdbc:postgresql://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/" + dbSettings.getDatabaseName()
        + "?autoReconnect=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8");
    }
}
