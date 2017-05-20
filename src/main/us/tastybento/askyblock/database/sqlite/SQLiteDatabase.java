package us.tastybento.askyblock.database.sqlite;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import us.tastybento.askyblock.database.ASBDatabase;
import us.tastybento.askyblock.database.objects.Island;
import us.tastybento.askyblock.database.objects.Player;

public class SQLiteDatabase extends ASBDatabase{

    @Override
    public Player loadPlayerData(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void savePlayerData(Player player) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Island loadIslandData(String location) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveIslandData(Island island) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HashMap<UUID, List<String>> loadOfflineHistoryMessages() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveOfflineHistoryMessages(HashMap<UUID, List<String>> messages) {
        // TODO Auto-generated method stub
        
    }

}
