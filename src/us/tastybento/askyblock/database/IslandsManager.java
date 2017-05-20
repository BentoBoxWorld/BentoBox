package us.tastybento.askyblock.database;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.askyblock.ASkyBlock;
import us.tastybento.askyblock.database.objects.Island;

public class IslandsManager {

    private ASkyBlock plugin;
    private ASBDatabase database;
    
    private HashMap<Location, Island> islands;
    private Island spawn;
    
    // Metrics data
    private int metrics_createdcount = 0;
    
    public IslandsManager(ASkyBlock plugin){
        this.plugin = plugin;
        database = ASBDatabase.getDatabase();
        islands = new HashMap<Location, Island>();
        spawn = null;
    }
    
    public void load(){
        //TODO
    }
    
    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                
                @Override
                public void run() {
                    for(Entry<Location, Island> entry : islands.entrySet()){
                        database.saveIslandData(entry.getValue());
                    }
                }
            });
        } else {
            for(Entry<Location, Island> entry : islands.entrySet()){
                database.saveIslandData(entry.getValue());
            }
        }
    }
    
    public void shutdown(){
        save(false);
        islands.clear();
    }
    
    public int getCount(){
        return islands.size();
    }
    
    public boolean isIsland(Location location){
        return islands.get(location) != null;
    }
    
    public Island getIsland(Location location){
        return islands.get(location);
    }
    
    public Island getIsland(UUID uuid){
        return plugin.getPlayers().getPlayer(uuid).getIsland();
    }
    
    public void createIsland(Location location){
        //TODO
    }
    
    public void deleteIsland(Location location){
        //TODO
    }
    
    public Island getSpawn(){
        return spawn;
    }
    
    // Metrics-related methods //
    
    public int metrics_getCreatedCount(){
        return metrics_createdcount;
    }
    
    public void metrics_setCreatedCount(int count){
        this.metrics_createdcount = count;
    }
}
