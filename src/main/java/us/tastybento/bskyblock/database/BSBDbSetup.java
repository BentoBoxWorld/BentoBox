package us.tastybento.bskyblock.database;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.mongodb.MongoDBDatabase;
import us.tastybento.bskyblock.database.mysql.MySQLDatabase;

public abstract class BSBDbSetup {

    /**
     * Gets the type of database being used. Currently supported options are
     * FLATFILE and MYSQL. Default is FLATFILE
     * @return Database type
     */
    public static BSBDbSetup getDatabase(){
        for(DatabaseType type : DatabaseType.values()){
            if(type == BSkyBlock.getInstance().getSettings().getDatabaseType()) {
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
     * @param dataObjectClass - class of the object to be stored in the database
     * @return handler for this database object
     */
    public abstract AbstractDatabaseHandler<?> getHandler(Class<?> dataObjectClass);

}