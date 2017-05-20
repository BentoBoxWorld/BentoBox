package us.tastybento.bskyblock.database.mysql;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.database.ASBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.APlayer;

public class MySQLDatabase extends ASBDatabase{

    @Override
    public APlayer loadPlayerData(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void savePlayerData(APlayer player) {
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
