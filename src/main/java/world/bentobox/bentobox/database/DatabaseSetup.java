package world.bentobox.bentobox.database;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.yaml.YamlDatabase;
import world.bentobox.bentobox.database.json.JSONDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;

public interface DatabaseSetup {

    /**
     * Gets the type of database being used.
     * Currently supported options are YAML, JSON, MYSQL and MONGODB.
     * Default is YAML.
     * @return Database type
     */
    static DatabaseSetup getDatabase() {
        for(DatabaseType type : DatabaseType.values()){
            if(type == BentoBox.getInstance().getSettings().getDatabaseType()) {
                return type.database;
            }
        }
        return DatabaseType.YAML.database;
    }

    enum DatabaseType {
        YAML(new YamlDatabase()),
        JSON(new JSONDatabase()),
        MYSQL(new MySQLDatabase()),
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