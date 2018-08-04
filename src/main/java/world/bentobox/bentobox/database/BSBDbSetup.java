package world.bentobox.bentobox.database;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.flatfile.FlatFileDatabase;
import world.bentobox.bentobox.database.mongodb.MongoDBDatabase;
import world.bentobox.bentobox.database.mysql.MySQLDatabase;

public abstract class BSBDbSetup {

    /**
     * Gets the type of database being used. Currently supported options are
     * FLATFILE and MYSQL. Default is FLATFILE
     * @return Database type
     */
    public static BSBDbSetup getDatabase(){
        for(DatabaseType type : DatabaseType.values()){
            if(type == BentoBox.getInstance().getSettings().getDatabaseType()) {
                return type.database;
            }
        }
        return DatabaseType.FLATFILE.database;
    }

    public enum DatabaseType{
        FLATFILE(new FlatFileDatabase()),
        MYSQL(new MySQLDatabase()),
        MONGO(new MongoDBDatabase());

        BSBDbSetup database;

        DatabaseType(BSBDbSetup database){
            this.database = database;
        }
    }

    /**
     * Gets a database handler that will store and retrieve classes of type dataObjectClass
     * @param <T> - Class type
     * @param dataObjectClass - class of the object to be stored in the database
     * @return handler for this database object
     */
    public abstract <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass);

}