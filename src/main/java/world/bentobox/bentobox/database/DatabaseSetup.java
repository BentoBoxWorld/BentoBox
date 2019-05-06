package world.bentobox.bentobox.database;

import java.util.Arrays;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mariadb.MariaDBDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.transitiondb.Yaml2JsonDatabase;

/**
 * @author Poslovitch
 */
public interface DatabaseSetup {

    /**
     * Gets the type of database being used.
     * Currently supported options are YAML, JSON, MYSQL, MARIADB and MONGODB.
     * Default is YAML.
     * @return Database type
     */
    static DatabaseSetup getDatabase() {
        BentoBox plugin = BentoBox.getInstance();
        return Arrays.stream(DatabaseType.values())
                .filter(plugin.getSettings().getDatabaseType()::equals)
                .findFirst()
                .map(t -> t.database)
                .orElse(DatabaseType.JSON.database);
    }

    enum DatabaseType {
        /*
         * Yaml now defaults to JSON
         * @since 1.5.0
         */
        YAML(new Yaml2JsonDatabase()),
        JSON(new JSONDatabase()),
        MYSQL(new MySQLDatabase()),
        /**
         * @since 1.1
         */
        MARIADB(new MariaDBDatabase()),
        MONGODB(new MongoDBDatabase());
        DatabaseSetup database;

        DatabaseType(DatabaseSetup database){
            this.database = database;
        }
    }

    /**
     * Gets a database handler that will store and retrieve classes of type dataObjectClass
     * @param <T> - Class type
     * @param dataObjectClass - class of the object to be stored in the database
     * @return handler for this database object
     */
    <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass);

}