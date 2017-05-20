package us.tastybento.bskyblock.database;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Player;

public class PlayersManager{
    
    private BSkyBlock plugin;
    private ASBDatabase database;
    
    private HashMap<UUID, Player> players;
    
    public PlayersManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = ASBDatabase.getDatabase();
        players = new HashMap<UUID, Player>();
    }
    
    public void load(){
        //TODO
    }
    
    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                
                @Override
                public void run() {
                    for(Entry<UUID, Player> entry : players.entrySet()){
                        database.savePlayerData(entry.getValue());
                    }
                }
            });
        } else {
            for(Entry<UUID, Player> entry : players.entrySet()){
                database.savePlayerData(entry.getValue());
            }
        }
    }
    
    public void shutdown(){
        save(false);
        players.clear();
    }
    
    public Player getPlayer(UUID uuid){
        return players.get(uuid);
    }
}
