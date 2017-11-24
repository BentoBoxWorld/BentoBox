package us.tastybento.bskyblock.database;

import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.mongodb.MongoDBDatabase;
import us.tastybento.bskyblock.database.mysql.MySQLDatabase;
import us.tastybento.bskyblock.database.sqlite.SQLiteDatabase;

public abstract class BSBDatabase {

    /**
     * Gets the type of database being used. Currently supported options are
     * FLATFILE and MYSQL. Default is FLATFILE
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
     * Gets a database handler that will store and retrieve classes of type dataObjectClass
     * @param plugin
     * @param dataObjectClass
     * @return database handler
     */
    public abstract AbstractDatabaseHandler<?> getHandler(Plugin plugin, Class<?> dataObjectClass);

}