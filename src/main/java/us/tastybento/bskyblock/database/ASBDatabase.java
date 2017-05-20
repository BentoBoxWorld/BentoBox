package us.tastybento.bskyblock.database;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.mysql.MySQLDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Player;
import us.tastybento.bskyblock.database.sqlite.SQLiteDatabase;

public abstract class ASBDatabase {
    
    public static ASBDatabase getDatabase(){
        for(DatabaseType type : DatabaseType.values()){
            if(type == Settings.databaseType) return type.database;
        }
        return DatabaseType.FLATFILE.database;
    }
    
    public abstract Player loadPlayerData(UUID uuid);
    public abstract void savePlayerData(Player player);
    
    public abstract Island loadIslandData(String location);
    public abstract void saveIslandData(Island island);
    
    public abstract HashMap<UUID, List<String>> loadOfflineHistoryMessages();
    public abstract void saveOfflineHistoryMessages(HashMap<UUID, List<String>> messages);
    
    public enum DatabaseType{
        FLATFILE(new FlatFileDatabase()),
        MYSQL(new MySQLDatabase()),
        SQLITE(new SQLiteDatabase());
        
        ASBDatabase database;
        
        DatabaseType(ASBDatabase database){
            this.database = database;
        }
    }
}
