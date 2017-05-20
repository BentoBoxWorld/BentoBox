package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.island.IslandLockEvent;
import us.tastybento.bskyblock.api.events.island.IslandUnlockEvent;
import us.tastybento.bskyblock.config.Settings;

/**
 * Stores all the info about an island
 * Managed by IslandsManager
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class Island {
    private BSkyBlock plugin;
    
    //// Island ////
    // The center of the island itself
    private Location center;
    // Island range
    private int range;
    
    // Display name
    private String name;
    
    // Time parameters
    private long createdDate;
    private long updatedDate;
    
    //// Team ////
    // Owner (Team Leader)
    private UUID owner;
    // Members
    private List<UUID> members;
    // Trustees
    private List<UUID> trustees;
    // Coops
    private List<UUID> coops;
    
    //// State ////
    private boolean locked = false;
    private boolean isSpawn = false;
    private boolean purgeProtected = false;
    
    //// Rating ////
    private int level;
    private int levelHandicap;
    
    //// Protection ////
    private HashMap<SettingsFlag, Boolean> flags = new HashMap<SettingsFlag, Boolean>();
    
    public Island(BSkyBlock plugin, int x, int z, UUID owner){
        
    }
    
    /**
     * @return the center Location
     */
    public Location getCenter(){
        return center;
    }
    
    /**
     * @return the x coordinate of the island center
     */
    public int getX(){
        return center.getBlockX();
    }
    
    /**
     * @return the y coordinate of the island center
     */
    public int getY(){
        return center.getBlockY();
    }
    
    /**
     * @return the z coordinate of the island center
     */
    public int getZ(){
        return center.getBlockZ();
    }
    
    /**
     * @return the island range
     */
    public int getRange(){
        return range;
    }
    
    /**
     * @param range - the range to set
     */
    public void setRange(int range){
        this.range = range;
    }
    
    /**
     * @return the island display name or the owner's name if none is set
     */
    public String getName(){
        return (name != null) ? name : plugin.getServer().getOfflinePlayer(owner).getName();
    }
    
    /**
     * @param name - the display name to set
     *               Set to null to remove the display name
     */
    public void setName(String name){
        this.name = name;
    }
    
    /**
     * @return the date when the island was created
     */
    public long getCreatedDate(){
        return createdDate;
    }
    
    /**
     * @param createdDate - the createdDate to sets
     */
    public void setCreatedDate(long createdDate){
        this.createdDate = createdDate;
    }
    
    /**
     * @return the date when the island was updated (team member connection, etc...)
     */
    public long getUpdatedDate(){
        return updatedDate;
    }
    
    /**
     * @param updatedDate - the updatedDate to sets
     */
    public void setUptadedDate(long updatedDate){
        this.updatedDate = updatedDate;
    }
    
    /**
     * @return the owner (team leader)
     */
    public UUID getOwner(){
        return owner;
    }
    
    /**
     * @param owner - the owner/team leader to set
     */
    public void setOwner(UUID owner){
        this.owner = owner;
    }
    
    /**
     * @return the members of the island (owner included)
     */
    public List<UUID> getMembers(){
        return members;
    }
    
    /**
     * @param members - the members to set
     */
    public void setMembers(List<UUID> members){
        this.members = members;
    }
    
    /**
     * @return the trustees players of the island
     */
    public List<UUID> getTrustees(){
        return trustees;
    }
    
    /**
     * @param trustees - the trustees to set
     */
    public void setTrustees(List<UUID> trustees){
        this.trustees = trustees;
    }
    
    /**
     * @return the coop players of the island
     */
    public List<UUID> getCoops(){
        return coops;
    }
    
    /**
     * @param coops - the coops to set
     */
    public void setCoops(List<UUID> coops){
        this.coops = coops;
    }
    
    /**
     * @return true if the island is locked, otherwise false
     */
    public boolean isLocked(){
        return locked;
    }
    
    /**
     * Locks/Unlocks the island. May be cancelled by
     * {@link IslandLockEvent} or {@link IslandUnlockEvent}.
     * @param locked - the lock state to set
     */
    public void setLocked(boolean locked){
        if(locked){
            // Lock the island
            IslandLockEvent event = new IslandLockEvent(this);
            plugin.getServer().getPluginManager().callEvent(event);
            
            if(!event.isCancelled()){
                this.locked = locked;
            }
        } else {
            // Unlock the island
            IslandUnlockEvent event = new IslandUnlockEvent(this);
            plugin.getServer().getPluginManager().callEvent(event);
            
            if(!event.isCancelled()){
                this.locked = locked;
            }
        }
    }
    
    /**
     * @return true if the island is the spawn otherwise false
     */
    public boolean isSpawn(){
        return isSpawn;
    }
    
    /**
     * @param isSpawn - if the island is the spawn
     */
    public void setSpawn(boolean isSpawn){
        this.isSpawn = isSpawn;
    }
    
    /**
     * @return true if the island is protected from the Purge, otherwise false
     */
    public boolean isPurgeProtected(){
        return purgeProtected;
    }
    
    /**
     * @param purgeProtected - if the island is protected from the Purge
     */
    public void setPurgeProtected(boolean purgeProtected){
        this.purgeProtected = purgeProtected;
    }
    
    /**
     * @return the island level
     */
    public int getLevel(){
        return level;
    }
    
    /**
     * @param level - the island level to set
     */
    public void setLevel(int level){
        this.level = level;
    }
    
    /**
     * @return the island level handicap
     */
    public int getLevelHandicap(){
        return levelHandicap;
    }
    
    /**
     * @param levelHandicap - the island level handicap to set
     */
    public void setLevelHandicap(int levelHandicap){
        this.levelHandicap = levelHandicap;
    }
    
    /**
     * Island Guard Settings flags
     * Covers island, spawn and system settings
     * 
     * @author Tastybento
     */
    public enum SettingsFlag{
        // Can use Anvil
        ANVIL,
        
        // Can interact with Armor Stand
        ARMOR_STAND,
        
        // Can interact with Beacon
        BEACON,
        
        // Can use bed
        BED,
        
        // Can break blocks
        BREAK_BLOCKS,
        
        // Can breed animals
        BREEDING,
        
        // Can use brewing stand
        BREWING,
        
        // Can use buttons
        BUTTON,
        
        // Can empty or fill buckets
        BUCKET,
        
        // Can collect lava (override BUCKET)
        COLLECT_LAVA,
        
        // Can collect water (override BUCKET)
        COLLECT_WATER,
        
        // Can eat and teleport with Chorus Fruit
        CHORUS_FRUIT,
        
        // Can use the workbench
        CRAFTING,
        
        // Allow creepers to hurt entities (but not to destroy blocks)
        CREEPER_HURT,
        
        // Allow creepers to destroy blocks
        CREEPER_GRIEFING,
        
        // Allow creepers to blow up chests (only if creeper_griefing is enabled)
        CREEPER_BLOW_UP_CHEST,
        
        // Allow creepers to blow up dispensers (only if creeper_griefing is enabled)
        CREEPER_BLOW_UP_DISPENSER,
        
        // Allow creepers to blow up droppers (only if creeper_griefing is enabled)
        CREEPER_BLOW_UP_DROPPER,
        
        // Allow creepers to blow up hoppers (only if creeper_griefing is enabled)
        CREEPER_BLOW_UP_HOPPER,
        
        // Allow creepers to blow up shulker boxes (only if creeper_griefing is enabled)
        CREEPER_BLOW_UP_SHULKER_BOX,
        
        // Can trample crops
        CROP_TRAMPLE,
        
        // Can open doors or trapdoors
        DOOR,
        
        // Can use Elytras
        ELYTRA,
        
        // Can use the enchanting table
        ENCHANTING,
        
        // Allow Enderman griefing
        ENDERMAN_GRIEFING,
        
        // Display enter/exit island messages
        ENTER_EXIT_MESSAGES,
        
        // Fire use/placement in general
        FIRE,
        
        // Can extinguish fires by punching them
        FIRE_EXTINGUISH,
        
        // Allow fire spread
        FIRE_SPREAD,
        
        // Can use fishing rod
        FISHING_ROD,
        
        // Can use furnaces
        FURNACE,
        
        // Can open gates
        GATE,
        
        // Can hurt animals (e.g. cows) - Villagers excluded
        HURT_ANIMALS,
        
        // Can hurt monsters
        HURT_MONSTERS,
        
        // Can hurt villagers
        HURT_VILLAGERS,
        
        // Can ignite creepers using flint and steel
        IGNITE_CREEPER,
        
        // Can ignite TNTs using flint and steel
        IGNITE_TNT,
        
        // Can interact with tamed animals
        INTERACT_TAMED,
        
        // Can drop items
        ITEM_DROP,
        
        // Can pickup items
        ITEM_PICKUP,
        
        // Can leash or unleash animals
        LEASH,
        
        // Can use levers
        LEVER,
        
        // Can milk cows
        MILKING,
        
        // Animals can spawn
        ANIMAL_SPAWN,
        
        // Monster projectiles can destroy item frames (skeleten arrows, shulker shots)
        MONSTER_DESTROY_ITEM_FRAMES,
        
        // Monsters can spawn
        MONSTER_SPAWN,
        
        // Can open horse or other animal inventories (llama)
        MOUNT_INVENTORY,
        
        // Can ride an animal
        MOUNT_RIDING,
        
        // Can operate jukeboxes, noteblocks
        MUSIC,
        
        // Can open chests
        OPEN_CHESTS,
        
        // Can open dispensers
        OPEN_DISPENSERS,
        
        // Can open droppers
        OPEN_DROPPERS,
        
        // Can open hoppers
        OPEN_HOPPERS,
        
        // Can open shulker boxes
        OPEN_SHULKER_BOXES,
        
        // Can place blocks
        PLACE_BLOCKS,
        
        // Can go through portals
        PORTAL,
        
        // Can activate pressure plates
        PRESSURE_PLATE,
                
        // Can do PvP in the overworld
        PVP_OVERWORLD,
        
        // Can do PvP in the nether
        PVP_NETHER,
        
        // Can interact with redstone items (repeaters, comparators)
        REDSTONE,
        
        // Can use spawn eggs
        SPAWN_EGGS,
        
        // Can shear sheeps
        SHEARING,
        
        // Can throw chicken eggs
        THROW_EGGS,
        
        // Can throw fireworks
        THROW_FIREWORKS,
        
        // Can throw enderpearls
        THROW_ENDERPEARLS,
        
        // Can throw snowballs
        THROW_SNOWBALLS,
        
        // Can throw splash potions
        THROW_SPLASH_POTIONS,
        
        // Allow TNT to hurt entities (but not to destroy blocks)
        TNT_HURT,
        
        // Allow TNT to destroy blocks
        TNT_GRIEFING,
        
        // Allow TNTs to blow up chests (only if TNT_griefing is enabled)
        TNT_BLOW_UP_CHEST,
        
        // Allow TNTs to blow up dispensers (only if TNT_griefing is enabled)
        TNT_BLOW_UP_DISPENSER,
        
        // Allow TNTs to blow up droppers (only if TNT_griefing is enabled)
        TNT_BLOW_UP_DROPPER,
        
        // Allow TNTs to blow up hoppers (only if TNT_griefing is enabled)
        TNT_BLOW_UP_HOPPER,
        
        // Allow TNTs to blow up shulker boxes (only if TNT_griefing is enabled)
        TNT_BLOW_UP_SHULKER_BOX,
        
        // Can trade with villagers
        VILLAGER_TRADING,
        
        // Allow Withers to hurt entities (but not to destroy blocks)
        WITHER_HURT,
        
        // Allow Wither to destroy blocks
        WITHER_GRIEFING,
        
        // Allow withers to blow up chests (only if wither_griefing is enabled)
        WITHER_BLOW_UP_CHEST,
        
        // Allow withers to blow up dispensers (only if wither_griefing is enabled)
        WITHER_BLOW_UP_DISPENSER,
        
        // Allow withers to blow up droppers (only if wither_griefing is enabled)
        WITHER_BLOW_UP_DROPPER,
        
        // Allow withers to blow up hoppers (only if wither_griefing is enabled)
        WITHER_BLOW_UP_HOPPER,
        
        // Allow withers to blow up shulker boxes (only if wither_griefing is enabled)
        WITHER_BLOW_UP_SHULKER_BOX
    }
    
    /**
     * Resets the flags to their default as set in config.yml for this island
     */
    public void setFlagsDefaults(){
        for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultIslandSettings.get(flag));
        }
    }
    
    /**
     * Resets the flags to their default as set in config.yml for the spawn
     */
    public void setSpawnFlagsDefaults(){
        for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultSpawnSettings.get(flag));
        }
    }
    
    /**
     * Get the Island Guard flag status
     * @param flag
     * @return true or false, or false if flag is not in the list
     */
    public boolean getFlag(SettingsFlag flag){
        if(flags.containsKey(flag)) return flags.get(flag);
        else return false;
    }
    
    /**
     * Set the Island Guard flag status
     * @param flag
     * @param value
     */
    public void setFlag(SettingsFlag flag, boolean value){
        flags.put(flag, value);
    }
    
    /**
     * Toggles the Island Guard flag status if it is in the list
     * @param flag
     */
    public void toggleFlag(SettingsFlag flag){
        if(flags.containsKey(flag)) flags.put(flag, (flags.get(flag)) ? false : true);
    }
}
