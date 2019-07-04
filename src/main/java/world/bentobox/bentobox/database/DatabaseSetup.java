package world.bentobox.bentobox.database;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mariadb.MariaDBDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;
import world.bentobox.bentobox.database.sqlite.SQLiteDatabase;
import world.bentobox.bentobox.database.transition.*;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

import java.util.Arrays;

/**
 * @author Poslovitch, tastybento
 */
public interface DatabaseSetup {

    /**
     * Gets the type of database being used.
     * Currently supported options are YAML, JSON, MYSQL, MARIADB and MONGODB.
     * Default is JSON.
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
         * Transition database, from YAML to MySQL (MariaDB)
         * @since 1.5.0
         */
        YAML2MARIADB(new Yaml2MariaDBDatabase()),

        /**
         * Transition database, from YAML to MongoDB
         * @since 1.6.0
         */
        YAML2MONGODB(new Yaml2MongoDBDatabase()),

        /**
         * Transition database, from YAML to SQLite
         * @since 1.6.0
         */
        YAML2SQLITE(new Yaml2SQLiteDatabase()),

        JSON(new JSONDatabase()),
        /**
         * Transition database, from JSON to MySQL
         * @since 1.5.0
         */
        JSON2MYSQL(new Json2MySQLDatabase()),
        /**
         * Transition database, from JSON to MySQL (MariaDB)
         * @since 1.5.0
         */
        JSON2MARIADB(new Json2MariaDBDatabase()),

        /**
         * Transition database, from JSON to MongoDB
         * @since 1.6.0
         */
        JSON2MONGODB(new Json2MongoDBDatabase()),

        /**
         * Transition database, from JSON to SQLite
         * @since 1.6.0
         */
        JSON2SQLITE(new Json2SQLiteDatabase()),

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

        /**
         * Transition database, from MariaDB to JSON
         * @since 1.6.0
         */
        MARIADB2JSON(new MariaDB2JsonDatabase()),

        MONGODB(new MongoDBDatabase()),

        /**
         * Transition database, from MongoDB to JSON
         * @since 1.6.0
         */
        MONGODB2JSON(new MongoDB2JsonDatabase()),

        /**
         * @since 1.6.0
         */
        SQLITE(new SQLiteDatabase()),

        /**
         * Transition database, from SQLite to JSON
         * @since 1.6.0
         */
        SQLITE2JSON(new SQLite2JsonDatabase());

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