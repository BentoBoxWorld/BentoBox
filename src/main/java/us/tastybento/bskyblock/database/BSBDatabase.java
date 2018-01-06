package us.tastybento.bskyblock.database;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.mysql.MySQLDatabase;

public abstract class BSBDatabase {

    /**
     * Gets the type of database being used. Currently supported options are
     * FLATFILE and MYSQL. Default is FLATFILE
     * @return Database type
     */
    public static BSBDatabase getDatabase(BSkyBlock plugin){
        for(DatabaseType type : DatabaseType.values()){
            if(type == plugin.getSettings().getDatabaseType()) return type.database;
        }
        return DatabaseType.FLATFILE.database;
    }

    public enum DatabaseType{
        FLATFILE(new FlatFileDatabase()),
        MYSQL(new MySQLDatabase());

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
    public abstract AbstractDatabaseHandler<?> getHandler(BSkyBlock plugin, Class<?> dataObjectClass);

}