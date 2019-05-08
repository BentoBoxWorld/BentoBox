package world.bentobox.bentobox.database;

import java.util.Arrays;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mariadb.MariaDBDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.transitiondb.Json2MySQLDatabase;
import world.bentobox.bentobox.database.transitiondb.MySQL2JsonDatabase;
import world.bentobox.bentobox.database.transitiondb.Yaml2JsonDatabase;
import world.bentobox.bentobox.database.transitiondb.Yaml2MariaDBDatabase;
import world.bentobox.bentobox.database.transitiondb.Yaml2MySQLDatabase;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

/**
 * @author Poslovitch, tastybento
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

    /**
     * Database types
     *
     */
    enum DatabaseType {
        YAML(new YamlDatabase()),
        /**
         * Transition database, from YAML to JSON
         * @since 1.5.0
         */
        YAML2JSON(new Yaml2JsonDatabase()),
        /**
         * Transition database, from YAML to MySQL
         * @since 1.5.0
         */
        YAML2MYSQL(new Yaml2MySQLDatabase()),
        /**
         * Transition database, from YAML to MySQL
         * @since 1.5.0
         */
        YAML2MARIA(new Yaml2MariaDBDatabase()),

        JSON(new JSONDatabase()),
        /**
         * Transition database, from JSON to MySQL
         * @since 1.5.0
         */
        JSON2MYSQL(new Json2MySQLDatabase()),

        MYSQL(new MySQLDatabase()),
        /**
         * Transition database, from MySQL to JSON
         * @since 1.5.0
         */
        MYSQL2JSON(new MySQL2JsonDatabase()),
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