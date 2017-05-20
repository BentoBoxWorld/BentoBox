package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.config.Settings;

/**
 * Tracks info on the player, recognized by his UUID
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class Player {
    private UUID uuid;
    
    private String locale;
    private boolean useControlPanel;
    private Island island;
    private int deaths;
    private int resetsLeft;
    private HashMap<Integer, Location> homes;
    
    /**
     * Constructor - initializes the variables
     * @param uuid - UUID of the player
     */
    public Player(UUID uuid){
        this.uuid = uuid;
        
        this.locale = "";
        this.useControlPanel = Settings.useControlPanel;
        this.island = null;
        this.deaths = 0;
        this.resetsLeft = Settings.defaultResetLimit;
        this.homes = new HashMap<Integer, Location>();
    }
    
    /**
     * @return player UUID
     */
    public UUID getUUID(){
        return uuid;
    }
    
    /**
     * @return the player locale
     */
    public String getLocale(){
        return locale;
    }
    
    /**
     * @param localeID - the locale to set
     */
    public void setLocale(String localeID){
        this.locale = localeID;
    }
    
    /**
     * @return if the player uses the control panel
     */
    public boolean useControlPanel(){
        return useControlPanel;
    }
    
    /**
     * @param useControlPanel - display the control panel to the player when /is or not
     */
    public void setUseControlPanel(boolean useControlPanel){
        this.useControlPanel = useControlPanel;
    }
    
    /**
     * @return the player island, or null if he don't have one
     */
    public Island getIsland(){
        return island;
    }
    
    /**
     * @param island - the island to set
     */
    public void setIsland(Island island){
        this.island = island;
    }
    
    /**
     * @return the player island location
     */
    public Location getIslandLocation(){
        return island.getCenter();
    }
    
    /**
     * @return if the player has an island (solo or in team)
     */
    public boolean hasIsland(){
        return island != null;
    }
    
    /**
     * @return if the player is the owner (team leader) of his island
     */
    public boolean isOwner(){
        return island.getOwner().equals(uuid);
    }
    
    /**
     * @return if the player is in a team
     */
    public boolean inTeam(){
        return island.getMembers().contains(uuid);
    }
    
    /**
     * @return the island team leader
     */
    public UUID getTeamLeader(){
        return island.getOwner();
    }
    
    /**
     * @return the player death count
     */
    public int getDeathCount(){
        return deaths;
    }
    
    /**
     * @param deaths - the death count to set
     */
    public void setDeathCount(int deaths){
        this.deaths = deaths;
    }
    
    /**
     * @return the player resets left
     */
    public int getResetsLeft(){
        return resetsLeft;
    }
    
    /**
     * @param resetsLeft - the resets left to set
     */
    public void setResetsLeft(int resetsLeft){
        this.resetsLeft = resetsLeft;
    }
    
    /**
     * @return the player homes
     */
    public HashMap<Integer, Location> getHomes(){
        return homes;
    }
    
    /**
     * @param homes - the homes to set
     */
    public void setHomes(HashMap<Integer, Location> homes){
        this.homes = homes;
    }
}
