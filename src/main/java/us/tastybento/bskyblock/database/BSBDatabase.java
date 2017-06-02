package us.tastybento.bskyblock.database;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.mongodb.MongoDBDatabase;
import us.tastybento.bskyblock.database.mysql.MySQLDatabase;
import us.tastybento.bskyblock.database.sqlite.SQLiteDatabase;

public abstract class BSBDatabase {
    
    /**
     * Gets the type of database being used. Currently supported options are
     * FLATFILE, MYSQL and SQLITE. Default is FLATFILE
     * @return Database type
     */
    public static BSBDatabase getDatabase(){
        for(DatabaseType type : DatabaseType.values()){
            if(type == Settings.databaseType) return type.database;
        }
        return DatabaseType.FLATFILE.database;
    }
    
    public enum DatabaseType{
        FLATFILE(new FlatFileDatabase()),
        MONGODB(new MongoDBDatabase()),
        MYSQL(new MySQLDatabase()),
        SQLITE(new SQLiteDatabase());
        
        BSBDatabase database;
        
        DatabaseType(BSBDatabase database){
            this.database = database;
        }
    }
    
    /**
     * Gets a handler for this class type with this database connection
     * @param plugin
     * @param type
     * @return selector object
     */
    public abstract AbstractDatabaseHandler<?> getHandler(BSkyBlock plugin, Class<?> type);
    
}