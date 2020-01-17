package world.bentobox.bentobox.database.sql.postgresql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.sql.SQLConfiguration;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;

/**
 *
 * @param <T>
 *
 * @since 1.6.0
 * @author tastybento, Poslovitch
 */
public class PostgreSQLDatabaseHandler<T> extends SQLDatabaseHandler<T> {

    /**
     * Constructor
     *
     * @param plugin            BentoBox plugin
     * @param type              The type of the objects that should be created and filled with
     *                          values from the database or inserted into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    PostgreSQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector, new SQLConfiguration(type.getCanonicalName())
                .schema("CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() 
                        + "` (uniqueId VARCHAR(255) NOT NULL PRIMARY KEY, json json NOT NULL)")
                .saveObject("INSERT INTO `" + type.getCanonicalName() + "` (json) VALUES (?) ON CONFLICT (uniqueId) DO UPDATE " +
                        "SET json = ?")
                );
        /*
         * TODO complete
        saveObjectSQL = "INSERT INTO `" + canonicalName + "` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?";
        deleteObjectSQL = "DELETE FROM `" + canonicalName + "` WHERE uniqueId = ?";
        objectExistsSQL = "SELECT IF ( EXISTS( SELECT * FROM `" + canonicalName + "` WHERE `uniqueId` = ?), 1, 0)";
    */
        }

}
