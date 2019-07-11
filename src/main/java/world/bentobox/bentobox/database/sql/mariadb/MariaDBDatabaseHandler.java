package world.bentobox.bentobox.database.sql.mariadb;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;
import world.bentobox.bentobox.database.sql.SQLConfiguration;

/**
 *
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento, barpec12
 *
 * @param <T>
 */
public class MariaDBDatabaseHandler<T> extends SQLDatabaseHandler<T> {

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param databaseConnector - authentication details for the database
     */
    MariaDBDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector, new SQLConfiguration(type.getCanonicalName())
                .schema("CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() +
                        "` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (JSON_EXTRACT(json, \"$.uniqueId\")), UNIQUE INDEX i (uniqueId))"));
    }
}
