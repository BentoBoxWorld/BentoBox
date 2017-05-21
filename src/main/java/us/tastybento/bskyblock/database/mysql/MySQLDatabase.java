package us.tastybento.bskyblock.database.mysql;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.database.ASBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Players;

public class MySQLDatabase extends ASBDatabase{

    @Override
    public Players loadPlayerData(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void savePlayerData(Players player) {
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

    @Override
    public boolean isPlayerKnown(UUID uniqueID) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UUID getUUID(String name, boolean adminCheck) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void savePlayerName(String name, UUID uuid) {
        // TODO Auto-generated method stub
        
    }

}
