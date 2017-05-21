package us.tastybento.bskyblock.database.flatfile;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import us.tastybento.bskyblock.database.ASBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Players;

public class FlatFileDatabase extends ASBDatabase{

    @Override
    public UUID getUUID(String name, boolean adminCheck) {
        /*
        if (plugin.getTinyDB() != null && plugin.getTinyDB().isDbReady()) {
            UUID result = plugin.getTinyDB().getPlayerUUID(string);
            if (result != null) {
                return result;
            }
        }
        // This goes after the database because it is possible for islands that have a duplicate name to be in
        // the cache. For example, Bill had an island but left. Bill changes his name to Bob. Then Alice changes
        // her name to Bill and logs into the game. There are now two islands with owner names called "Bill"
        // The name database will ensure the names are updated.
        for (UUID id : playerCache.keySet()) {
            String name = playerCache.get(id).getPlayerName();
            //plugin.getLogger().info("DEBUG: Testing name " + name);
            if (name != null && name.equalsIgnoreCase(string)) {
                //plugin.getLogger().info("DEBUG: found it! " + id);
                return id;
            }
        }
        */
        // Try the server
        if (adminCheck) {
            return Bukkit.getServer().getOfflinePlayer(name).getUniqueId();
        }
        return null;
    }

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
    public void savePlayerName(String name, UUID uuid) {
        // TODO Auto-generated method stub
        
    }
}
