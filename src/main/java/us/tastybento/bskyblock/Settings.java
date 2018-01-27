package us.tastybento.bskyblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import us.tastybento.bskyblock.Constants.GameType;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.ISettings;
import us.tastybento.bskyblock.api.configuration.PotionEffectListAdpater;
import us.tastybento.bskyblock.api.configuration.StoreAt;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.BSBDatabase.DatabaseType;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
public class Settings implements ISettings<Settings> {
        
    // ---------------------------------------------

    /*      GENERAL     */

    @ConfigEntry(path = "general.check-updates")
    private boolean checkUpdates = true;

    @ConfigEntry(path = "general.default-language")
    private String defaultLanguage = "en-US";

    @ConfigEntry(path = "general.use-economy")
    private boolean useEconomy = true;

    // Purge
    @ConfigEntry(path = "general.purge.max-island-level")
    private int purgeMaxIslandLevel = 50;

    @ConfigEntry(path = "general.purge.remove-user-data")
    private boolean purgeRemoveUserData = false;

    // Database
    @ConfigEntry(path = "general.database.type")
    private DatabaseType databaseType = DatabaseType.FLATFILE;

    @ConfigEntry(path = "general.database.host")
    private String dbHost = "localhost";

    @ConfigEntry(path = "general.database.port")
    private int dbPort = 3306;

    @ConfigEntry(path = "general.database.name")
    private String dbName = "BSkyBlock";

    @ConfigEntry(path = "general.database.username")
    private String dbUsername = "username";

    @ConfigEntry(path = "general.database.password")
    private String dbPassword = "password";

    @ConfigEntry(path = "general.database.backup-period")
    private int databaseBackupPeriod = 5;

    //TODO change allowAutoActivator to the fakePlayers introduced in ASB 3.0.8
    @ConfigEntry(path = "general.allow-FTB-auto-activators")
    private boolean allowAutoActivator = false;

    @ConfigEntry(path = "general.allow-obsidian-scooping")
    private boolean allowObsidianScooping = true;

    // ---------------------------------------------

    /*      WORLD       */
    @ConfigEntry(path = "world.world-name", needsReset = true)
    private String worldName = "BSkyBlock";

    @ConfigEntry(path = "world.distance-between-islands", needsReset = true)
    private int islandDistance = 200;

    @ConfigEntry(path = "world.protection-range", overrideOnChange = true)
    private int islandProtectionRange = 100;

    @ConfigEntry(path = "world.start-x", needsReset = true)
    private int islandStartX = 0;

    @ConfigEntry(path = "world.start-z", needsReset = true)
    private int islandStartZ = 0;

    private int islandXOffset;
    private int islandZOffset;

    @ConfigEntry(path = "world.sea-height")
    private int seaHeight = 0;

    @ConfigEntry(path = "world.island-height")
    private int islandHeight = 100;

    @ConfigEntry(path = "world.max-islands")
    private int maxIslands = -1;

    // Nether
    @ConfigEntry(path = "world.nether.generate")
    private boolean netherGenerate = true;

    @ConfigEntry(path = "world.nether.islands", needsReset = true)
    private boolean netherIslands = true;

    @ConfigEntry(path = "world.nether.trees")
    private boolean netherTrees = true;

    @ConfigEntry(path = "world.nether.roof")
    private boolean netherRoof = true;

    @ConfigEntry(path = "world.nether.spawn-radius")
    private int netherSpawnRadius = 32;

    // End
    @ConfigEntry(path = "world.end.generate")
    private boolean endGenerate = true;

    @ConfigEntry(path = "world.end.islands", needsReset = true)
    private boolean endIslands = true;

    // Entities
    private HashMap<EntityType, Integer> entityLimits;
    private HashMap<String, Integer> tileEntityLimits;

    // ---------------------------------------------

    /*      ISLAND      */
    private int maxTeamSize;
    private int maxHomes;
    private int nameMinLength;
    private int nameMaxLength;
    private int inviteWait;

    // Reset
    private int resetLimit;
    @ConfigEntry(path = "island.require-confirmation.reset")
    private boolean resetConfirmation;

    @ConfigEntry(path = "island.require-confirmation.reset-wait")
    private long resetWait;

    private boolean leaversLoseReset;
    private boolean kickedKeepInventory;

    // Remove mobs
    private boolean removeMobsOnLogin;
    private boolean removeMobsOnIsland;
    private List<String> removeMobsWhitelist = new ArrayList<>();

    private boolean makeIslandIfNone;
    private boolean immediateTeleportOnIsland;
    private boolean respawnOnIsland;

    // Deaths
    @ConfigEntry(path = "island.deaths.max")
    private int deathsMax = 10;

    @ConfigEntry(path = "island.deaths.sum-team")
    private boolean deathsSumTeam = false;

    // ---------------------------------------------

    /*      PROTECTION      */
    @ConfigEntry(path = "protection.allow-piston-push")
    private boolean allowPistonPush = false;

    @ConfigEntry(path = "protection.restrict-flying-mobs")
    private boolean restrictFlyingMobs = true;

    private int togglePvPCooldown;

    private HashMap<Flag, Boolean> defaultFlags;

    //TODO transform these options below into flags
    private boolean allowEndermanGriefing;
    private boolean endermanDeathDrop;
    private boolean allowTNTDamage;
    private boolean allowChestDamage;
    private boolean allowCreeperDamage;
    private boolean allowCreeperGriefing;
    private boolean allowMobDamageToItemFrames;

    //TODO flags

    // ---------------------------------------------

    /*      ACID        */

    /*
     * This settings category only exists if the GameType is ACIDISLAND.
     */

    @ConfigEntry(path = "acid.damage-op", specificTo = GameType.ACIDISLAND)
    private boolean acidDamageOp = false;

    @ConfigEntry(path = "acid.damage-chickens", specificTo = GameType.ACIDISLAND)
    private boolean acidDamageChickens = false;

    @ConfigEntry(path = "acid.options.item-destroy-time", specificTo = GameType.ACIDISLAND)
    private int acidDestroyItemTime = 0;

    // Damage
    @ConfigEntry(path = "acid.damage.acid.player", specificTo = GameType.ACIDISLAND)
    private int acidDamage = 10;

    @ConfigEntry(path = "acid.damage.rain", specificTo = GameType.ACIDISLAND)
    private int acidRainDamage = 1;

    @ConfigEntry(path = "acid.damage.effects", specificTo = GameType.ACIDISLAND, adapter = PotionEffectListAdpater.class)
    private List<PotionEffectType> acidEffects = new ArrayList<>(Arrays.asList(PotionEffectType.CONFUSION, PotionEffectType.SLOW));

    /*      SCHEMATICS      */
    private List<String> companionNames = new ArrayList<>();
    
    @ConfigEntry(path = "island.chest-items")
    private List<ItemStack> chestItems = new ArrayList<>();
    
    private EntityType companionType = EntityType.COW;

    private boolean useOwnGenerator;

    private HashMap<String,Integer> limitedBlocks;
    private boolean teamJoinDeathReset;
    
    private String uniqueId = "config";

    // Timeout for team kick and leave commands
    @ConfigEntry(path = "island.require-confirmation.kick")
    private boolean kickConfirmation;

    @ConfigEntry(path = "island.require-confirmation.kick-wait")
    private long kickWait;
 
    @ConfigEntry(path = "island.require-confirmation.leave")
    private boolean leaveConfirmation;

    @ConfigEntry(path = "island.require-confirmation.leave-wait")
    private long leaveWait;
    

    /**
     * @return the acidDamage
     */
    public int getAcidDamage() {
        return acidDamage;
    }
    /**
     * @return the acidDestroyItemTime
     */
    public int getAcidDestroyItemTime() {
        return acidDestroyItemTime;
    }
    /**
     * @return the acidEffects
     */
    public List<PotionEffectType> getAcidEffects() {
        return acidEffects;
    }
    /**
     * @return the acidRainDamage
     */
    public int getAcidRainDamage() {
        return acidRainDamage;
    }
    /**
     * @return the chestItems
     */
    public List<ItemStack> getChestItems() {
        return chestItems;
    }
    /**
     * @return the companionNames
     */
    public List<String> getCompanionNames() {
        return companionNames;
    }
    /**
     * @return the companionType
     */
    public EntityType getCompanionType() {
        return companionType;
    }
    /**
     * @return the databaseBackupPeriod
     */
    public int getDatabaseBackupPeriod() {
        return databaseBackupPeriod;
    }
    /**
     * @return the databaseType
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    /**
     * @return the dbHost
     */
    public String getDbHost() {
        return dbHost;
    }
    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }
    /**
     * @return the dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }
    /**
     * @return the dbPort
     */
    public int getDbPort() {
        return dbPort;
    }
    /**
     * @return the dbUsername
     */
    public String getDbUsername() {
        return dbUsername;
    }
    /**
     * @return the deathsMax
     */
    public int getDeathsMax() {
        return deathsMax;
    }
    /**
     * @return the defaultFlags
     */
    public HashMap<Flag, Boolean> getDefaultFlags() {
        return defaultFlags;
    }
    /**
     * @return the defaultLanguage
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * @return the entityLimits
     */
    public HashMap<EntityType, Integer> getEntityLimits() {
        return entityLimits;
    }
    @Override
    public Settings getInstance() {
        // TODO Auto-generated method stub
        return this;
    }
    /**
     * @return the inviteWait
     */
    public int getInviteWait() {
        return inviteWait;
    }
    /**
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return islandDistance;
    }
    /**
     * @return the islandHeight
     */
    public int getIslandHeight() {
        return islandHeight;
    }
    /**
     * @return the islandProtectionRange
     */
    public int getIslandProtectionRange() {
        return islandProtectionRange;
    }
    /**
     * @return the islandStartX
     */
    public int getIslandStartX() {
        return islandStartX;
    }
    /**
     * @return the islandStartZ
     */
    public int getIslandStartZ() {
        return islandStartZ;
    }
    /**
     * @return the islandXOffset
     */
    public int getIslandXOffset() {
        return islandXOffset;
    }
    /**
     * @return the islandZOffset
     */
    public int getIslandZOffset() {
        return islandZOffset;
    }
    /**
     * @return the kickWait
     */
    public long getKickWait() {
        return kickWait;
    }
    /**
     * @return the leaveWait
     */
    public long getLeaveWait() {
        return leaveWait;
    }
    /**
     * @return the limitedBlocks
     */
    public HashMap<String, Integer> getLimitedBlocks() {
        return limitedBlocks;
    }
    /**
     * @return the maxHomes
     */
    public int getMaxHomes() {
        return maxHomes;
    }
    /**
     * @return the maxIslands
     */
    public int getMaxIslands() {
        return maxIslands;
    }
    /**
     * @return the maxTeamSize
     */
    public int getMaxTeamSize() {
        return maxTeamSize;
    }
    /**
     * @return the nameMaxLength
     */
    public int getNameMaxLength() {
        return nameMaxLength;
    }
    /**
     * @return the nameMinLength
     */
    public int getNameMinLength() {
        return nameMinLength;
    }
    /**
     * @return the netherSpawnRadius
     */
    public int getNetherSpawnRadius() {
        return netherSpawnRadius;
    }
    /**
     * @return the purgeMaxIslandLevel
     */
    public int getPurgeMaxIslandLevel() {
        return purgeMaxIslandLevel;
    }
    /**
     * @return the removeMobsWhitelist
     */
    public List<String> getRemoveMobsWhitelist() {
        return removeMobsWhitelist;
    }
    /**
     * @return the resetLimit
     */
    public int getResetLimit() {
        return resetLimit;
    }
    /**
     * @return the resetWait
     */
    public long getResetWait() {
        return resetWait;
    }
    /**
     * @return the seaHeight
     */
    public int getSeaHeight() {
        return seaHeight;
    }
    /**
     * @return the tileEntityLimits
     */
    public HashMap<String, Integer> getTileEntityLimits() {
        return tileEntityLimits;
    }
    /**
     * @return the togglePvPCooldown
     */
    public int getTogglePvPCooldown() {
        return togglePvPCooldown;
    }
    /**
     * @return the uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }
    /**
     * @return the worldName
     */
    public String getWorldName() {
        return worldName;
    }
    /**
     * @return the acidDamageChickens
     */
    public boolean isAcidDamageChickens() {
        return acidDamageChickens;
    }
    /**
     * @return the acidDamageOp
     */
    public boolean isAcidDamageOp() {
        return acidDamageOp;
    }
    /**
     * @return the allowAutoActivator
     */
    public boolean isAllowAutoActivator() {
        return allowAutoActivator;
    }
    /**
     * @return the allowChestDamage
     */
    public boolean isAllowChestDamage() {
        return allowChestDamage;
    }
    /**
     * @return the allowCreeperDamage
     */
    public boolean isAllowCreeperDamage() {
        return allowCreeperDamage;
    }
    /**
     * @return the allowCreeperGriefing
     */
    public boolean isAllowCreeperGriefing() {
        return allowCreeperGriefing;
    }
    /**
     * @return the allowEndermanGriefing
     */
    public boolean isAllowEndermanGriefing() {
        return allowEndermanGriefing;
    }
    /**
     * @return the allowMobDamageToItemFrames
     */
    public boolean isAllowMobDamageToItemFrames() {
        return allowMobDamageToItemFrames;
    }
    /**
     * @return the allowObsidianScooping
     */
    public boolean isAllowObsidianScooping() {
        return allowObsidianScooping;
    }
    /**
     * @return the allowPistonPush
     */
    public boolean isAllowPistonPush() {
        return allowPistonPush;
    }
    /**
     * @return the allowTNTDamage
     */
    public boolean isAllowTNTDamage() {
        return allowTNTDamage;
    }
    /**
     * @return the checkUpdates
     */
    public boolean isCheckUpdates() {
        return checkUpdates;
    }
    /**
     * @return the deathsSumTeam
     */
    public boolean isDeathsSumTeam() {
        return deathsSumTeam;
    }
    /**
     * @return the endermanDeathDrop
     */
    public boolean isEndermanDeathDrop() {
        return endermanDeathDrop;
    }
    /**
     * @return the endGenerate
     */
    public boolean isEndGenerate() {
        return endGenerate;
    }
    /**
     * @return the endIslands
     */
    public boolean isEndIslands() {
        return endIslands;
    }
    /**
     * @return the immediateTeleportOnIsland
     */
    public boolean isImmediateTeleportOnIsland() {
        return immediateTeleportOnIsland;
    }
    /**
     * @return the kickConfirmation
     */
    public boolean isKickConfirmation() {
        return kickConfirmation;
    }
    /**
     * @return the kickedKeepInventory
     */
    public boolean isKickedKeepInventory() {
        return kickedKeepInventory;
    }
    /**
     * @return the leaveConfirmation
     */
    public boolean isLeaveConfirmation() {
        return leaveConfirmation;
    }
    /**
     * @return the leaversLoseReset
     */
    public boolean isLeaversLoseReset() {
        return leaversLoseReset;
    }
    /**
     * @return the makeIslandIfNone
     */
    public boolean isMakeIslandIfNone() {
        return makeIslandIfNone;
    }
    /**
     * @return the netherGenerate
     */
    public boolean isNetherGenerate() {
        return netherGenerate;
    }
    /**
     * @return the netherIslands
     */
    public boolean isNetherIslands() {
        return netherIslands;
    }
    /**
     * @return the netherRoof
     */
    public boolean isNetherRoof() {
        return netherRoof;
    }
    /**
     * @return the netherTrees
     */
    public boolean isNetherTrees() {
        return netherTrees;
    }
    /**
     * @return the purgeRemoveUserData
     */
    public boolean isPurgeRemoveUserData() {
        return purgeRemoveUserData;
    }
    /**
     * @return the removeMobsOnIsland
     */
    public boolean isRemoveMobsOnIsland() {
        return removeMobsOnIsland;
    }
    /**
     * @return the removeMobsOnLogin
     */
    public boolean isRemoveMobsOnLogin() {
        return removeMobsOnLogin;
    }
    /**
     * @return the resetConfirmation
     */
    public boolean isResetConfirmation() {
        return resetConfirmation;
    }
    /**
     * @return the respawnOnIsland
     */
    public boolean isRespawnOnIsland() {
        return respawnOnIsland;
    }
    /**
     * @return the restrictFlyingMobs
     */
    public boolean isRestrictFlyingMobs() {
        return restrictFlyingMobs;
    }
    /**
     * @return the teamJoinDeathReset
     */
    public boolean isTeamJoinDeathReset() {
        return teamJoinDeathReset;
    }
    /**
     * @return the useEconomy
     */
    public boolean isUseEconomy() {
        return useEconomy;
    }
    /**
     * @return the useOwnGenerator
     */
    public boolean isUseOwnGenerator() {
        return useOwnGenerator;
    }
    /**
     * @param acidDamage the acidDamage to set
     */
    public void setAcidDamage(int acidDamage) {
        this.acidDamage = acidDamage;
    }
    /**
     * @param acidDamageChickens the acidDamageChickens to set
     */
    public void setAcidDamageChickens(boolean acidDamageChickens) {
        this.acidDamageChickens = acidDamageChickens;
    }
    /**
     * @param acidDamageOp the acidDamageOp to set
     */
    public void setAcidDamageOp(boolean acidDamageOp) {
        this.acidDamageOp = acidDamageOp;
    }
    /**
     * @param acidDestroyItemTime the acidDestroyItemTime to set
     */
    public void setAcidDestroyItemTime(int acidDestroyItemTime) {
        this.acidDestroyItemTime = acidDestroyItemTime;
    }
    /**
     * @param acidEffects the acidEffects to set
     */
    public void setAcidEffects(List<PotionEffectType> acidEffects) {
        this.acidEffects = acidEffects;
    }
    /**
     * @param acidRainDamage the acidRainDamage to set
     */
    public void setAcidRainDamage(int acidRainDamage) {
        this.acidRainDamage = acidRainDamage;
    }
    /**
     * @param allowAutoActivator the allowAutoActivator to set
     */
    public void setAllowAutoActivator(boolean allowAutoActivator) {
        this.allowAutoActivator = allowAutoActivator;
    }
    /**
     * @param allowChestDamage the allowChestDamage to set
     */
    public void setAllowChestDamage(boolean allowChestDamage) {
        this.allowChestDamage = allowChestDamage;
    }
    /**
     * @param allowCreeperDamage the allowCreeperDamage to set
     */
    public void setAllowCreeperDamage(boolean allowCreeperDamage) {
        this.allowCreeperDamage = allowCreeperDamage;
    }
    /**
     * @param allowCreeperGriefing the allowCreeperGriefing to set
     */
    public void setAllowCreeperGriefing(boolean allowCreeperGriefing) {
        this.allowCreeperGriefing = allowCreeperGriefing;
    }
    /**
     * @param allowEndermanGriefing the allowEndermanGriefing to set
     */
    public void setAllowEndermanGriefing(boolean allowEndermanGriefing) {
        this.allowEndermanGriefing = allowEndermanGriefing;
    }
    /**
     * @param allowMobDamageToItemFrames the allowMobDamageToItemFrames to set
     */
    public void setAllowMobDamageToItemFrames(boolean allowMobDamageToItemFrames) {
        this.allowMobDamageToItemFrames = allowMobDamageToItemFrames;
    }
    /**
     * @param allowObsidianScooping the allowObsidianScooping to set
     */
    public void setAllowObsidianScooping(boolean allowObsidianScooping) {
        this.allowObsidianScooping = allowObsidianScooping;
    }
    /**
     * @param allowPistonPush the allowPistonPush to set
     */
    public void setAllowPistonPush(boolean allowPistonPush) {
        this.allowPistonPush = allowPistonPush;
    }
    /**
     * @param allowTNTDamage the allowTNTDamage to set
     */
    public void setAllowTNTDamage(boolean allowTNTDamage) {
        this.allowTNTDamage = allowTNTDamage;
    }
    /**
     * @param checkUpdates the checkUpdates to set
     */
    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
    }
    /**
     * @param chestItems the chestItems to set
     */
    public void setChestItems(List<ItemStack> chestItems) {
        this.chestItems = chestItems;
    }
    /**
     * @param companionNames the companionNames to set
     */
    public void setCompanionNames(List<String> companionNames) {
        this.companionNames = companionNames;
    }
    /**
     * @param companionType the companionType to set
     */
    public void setCompanionType(EntityType companionType) {
        this.companionType = companionType;
    }
    /**
     * @param databaseBackupPeriod the databaseBackupPeriod to set
     */
    public void setDatabaseBackupPeriod(int databaseBackupPeriod) {
        this.databaseBackupPeriod = databaseBackupPeriod;
    }
    /**
     * @param databaseType the databaseType to set
     */
    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }
    /**
     * @param dbHost the dbHost to set
     */
    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }
    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
    /**
     * @param dbPort the dbPort to set
     */
    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }
    /**
     * @param dbUsername the dbUsername to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }
    /**
     * @param deathsMax the deathsMax to set
     */
    public void setDeathsMax(int deathsMax) {
        this.deathsMax = deathsMax;
    }
    /**
     * @param deathsSumTeam the deathsSumTeam to set
     */
    public void setDeathsSumTeam(boolean deathsSumTeam) {
        this.deathsSumTeam = deathsSumTeam;
    }
    /**
     * @param defaultFlags the defaultFlags to set
     */
    public void setDefaultFlags(HashMap<Flag, Boolean> defaultFlags) {
        this.defaultFlags = defaultFlags;
    }
    /**
     * @param defaultLanguage the defaultLanguage to set
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    /**
     * @param endermanDeathDrop the endermanDeathDrop to set
     */
    public void setEndermanDeathDrop(boolean endermanDeathDrop) {
        this.endermanDeathDrop = endermanDeathDrop;
    }
    /**
     * @param endGenerate the endGenerate to set
     */
    public void setEndGenerate(boolean endGenerate) {
        this.endGenerate = endGenerate;
    }
    /**
     * @param endIslands the endIslands to set
     */
    public void setEndIslands(boolean endIslands) {
        this.endIslands = endIslands;
    }
    /**
     * @param entityLimits the entityLimits to set
     */
    public void setEntityLimits(HashMap<EntityType, Integer> entityLimits) {
        this.entityLimits = entityLimits;
    }
    /**
     * @param immediateTeleportOnIsland the immediateTeleportOnIsland to set
     */
    public void setImmediateTeleportOnIsland(boolean immediateTeleportOnIsland) {
        this.immediateTeleportOnIsland = immediateTeleportOnIsland;
    }
    /**
     * @param inviteWait the inviteWait to set
     */
    public void setInviteWait(int inviteWait) {
        this.inviteWait = inviteWait;
    }
    /**
     * @param islandDistance the islandDistance to set
     */
    public void setIslandDistance(int islandDistance) {
        this.islandDistance = islandDistance;
    }
    /**
     * @param islandHeight the islandHeight to set
     */
    public void setIslandHeight(int islandHeight) {
        this.islandHeight = islandHeight;
    }
    /**
     * @param islandProtectionRange the islandProtectionRange to set
     */
    public void setIslandProtectionRange(int islandProtectionRange) {
        this.islandProtectionRange = islandProtectionRange;
    }
    /**
     * @param islandStartX the islandStartX to set
     */
    public void setIslandStartX(int islandStartX) {
        this.islandStartX = islandStartX;
    }
    /**
     * @param islandStartZ the islandStartZ to set
     */
    public void setIslandStartZ(int islandStartZ) {
        this.islandStartZ = islandStartZ;
    }
    /**
     * @param islandXOffset the islandXOffset to set
     */
    public void setIslandXOffset(int islandXOffset) {
        this.islandXOffset = islandXOffset;
    }
    /**
     * @param islandZOffset the islandZOffset to set
     */
    public void setIslandZOffset(int islandZOffset) {
        this.islandZOffset = islandZOffset;
    }
    /**
     * @param kickConfirmation the kickConfirmation to set
     */
    public void setKickConfirmation(boolean kickConfirmation) {
        this.kickConfirmation = kickConfirmation;
    }
    /**
     * @param kickedKeepInventory the kickedKeepInventory to set
     */
    public void setKickedKeepInventory(boolean kickedKeepInventory) {
        this.kickedKeepInventory = kickedKeepInventory;
    }
    /**
     * @param kickWait the kickWait to set
     */
    public void setKickWait(long kickWait) {
        this.kickWait = kickWait;
    }
    /**
     * @param leaveConfirmation the leaveConfirmation to set
     */
    public void setLeaveConfirmation(boolean leaveConfirmation) {
        this.leaveConfirmation = leaveConfirmation;
    }
    /**
     * @param leaversLoseReset the leaversLoseReset to set
     */
    public void setLeaversLoseReset(boolean leaversLoseReset) {
        this.leaversLoseReset = leaversLoseReset;
    }
    /**
     * @param leaveWait the leaveWait to set
     */
    public void setLeaveWait(long leaveWait) {
        this.leaveWait = leaveWait;
    }
    /**
     * @param limitedBlocks the limitedBlocks to set
     */
    public void setLimitedBlocks(HashMap<String, Integer> limitedBlocks) {
        this.limitedBlocks = limitedBlocks;
    }
    /**
     * @param makeIslandIfNone the makeIslandIfNone to set
     */
    public void setMakeIslandIfNone(boolean makeIslandIfNone) {
        this.makeIslandIfNone = makeIslandIfNone;
    }
    /**
     * @param maxHomes the maxHomes to set
     */
    public void setMaxHomes(int maxHomes) {
        this.maxHomes = maxHomes;
    }
    /**
     * @param maxIslands the maxIslands to set
     */
    public void setMaxIslands(int maxIslands) {
        this.maxIslands = maxIslands;
    }
    /**
     * @param maxTeamSize the maxTeamSize to set
     */
    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }
    /**
     * @param nameMaxLength the nameMaxLength to set
     */
    public void setNameMaxLength(int nameMaxLength) {
        this.nameMaxLength = nameMaxLength;
    }
    /**
     * @param nameMinLength the nameMinLength to set
     */
    public void setNameMinLength(int nameMinLength) {
        this.nameMinLength = nameMinLength;
    }
    /**
     * @param netherGenerate the netherGenerate to set
     */
    public void setNetherGenerate(boolean netherGenerate) {
        this.netherGenerate = netherGenerate;
    }
    /**
     * @param netherIslands the netherIslands to set
     */
    public void setNetherIslands(boolean netherIslands) {
        this.netherIslands = netherIslands;
    }
    /**
     * @param netherRoof the netherRoof to set
     */
    public void setNetherRoof(boolean netherRoof) {
        this.netherRoof = netherRoof;
    }
    /**
     * @param netherSpawnRadius the netherSpawnRadius to set
     */
    public void setNetherSpawnRadius(int netherSpawnRadius) {
        this.netherSpawnRadius = netherSpawnRadius;
    }
    /**
     * @param netherTrees the netherTrees to set
     */
    public void setNetherTrees(boolean netherTrees) {
        this.netherTrees = netherTrees;
    }
    /**
     * @param purgeMaxIslandLevel the purgeMaxIslandLevel to set
     */
    public void setPurgeMaxIslandLevel(int purgeMaxIslandLevel) {
        this.purgeMaxIslandLevel = purgeMaxIslandLevel;
    }
    /**
     * @param purgeRemoveUserData the purgeRemoveUserData to set
     */
    public void setPurgeRemoveUserData(boolean purgeRemoveUserData) {
        this.purgeRemoveUserData = purgeRemoveUserData;
    }
    /**
     * @param removeMobsOnIsland the removeMobsOnIsland to set
     */
    public void setRemoveMobsOnIsland(boolean removeMobsOnIsland) {
        this.removeMobsOnIsland = removeMobsOnIsland;
    }
    /**
     * @param removeMobsOnLogin the removeMobsOnLogin to set
     */
    public void setRemoveMobsOnLogin(boolean removeMobsOnLogin) {
        this.removeMobsOnLogin = removeMobsOnLogin;
    }
    /**
     * @param removeMobsWhitelist the removeMobsWhitelist to set
     */
    public void setRemoveMobsWhitelist(List<String> removeMobsWhitelist) {
        this.removeMobsWhitelist = removeMobsWhitelist;
    }
    
    /**
     * @param resetConfirmation the resetConfirmation to set
     */
    public void setResetConfirmation(boolean resetConfirmation) {
        this.resetConfirmation = resetConfirmation;
    }
    /**
     * @param resetLimit the resetLimit to set
     */
    public void setResetLimit(int resetLimit) {
        this.resetLimit = resetLimit;
    }
    /**
     * @param resetWait the resetWait to set
     */
    public void setResetWait(long resetWait) {
        this.resetWait = resetWait;
    }
    /**
     * @param respawnOnIsland the respawnOnIsland to set
     */
    public void setRespawnOnIsland(boolean respawnOnIsland) {
        this.respawnOnIsland = respawnOnIsland;
    }
    /**
     * @param restrictFlyingMobs the restrictFlyingMobs to set
     */
    public void setRestrictFlyingMobs(boolean restrictFlyingMobs) {
        this.restrictFlyingMobs = restrictFlyingMobs;
    }
    /**
     * @param seaHeight the seaHeight to set
     */
    public void setSeaHeight(int seaHeight) {
        this.seaHeight = seaHeight;
    }
    /**
     * @param teamJoinDeathReset the teamJoinDeathReset to set
     */
    public void setTeamJoinDeathReset(boolean teamJoinDeathReset) {
        this.teamJoinDeathReset = teamJoinDeathReset;
    }
    /**
     * @param tileEntityLimits the tileEntityLimits to set
     */
    public void setTileEntityLimits(HashMap<String, Integer> tileEntityLimits) {
        this.tileEntityLimits = tileEntityLimits;
    }
    /**
     * @param togglePvPCooldown the togglePvPCooldown to set
     */
    public void setTogglePvPCooldown(int togglePvPCooldown) {
        this.togglePvPCooldown = togglePvPCooldown;
    }
    /**
     * @param uniqueId the uniqueId to set
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    /**
     * @param useEconomy the useEconomy to set
     */
    public void setUseEconomy(boolean useEconomy) {
        this.useEconomy = useEconomy;
    }
    /**
     * @param useOwnGenerator the useOwnGenerator to set
     */
    public void setUseOwnGenerator(boolean useOwnGenerator) {
        this.useOwnGenerator = useOwnGenerator;
    }
    /**
     * @param worldName the worldName to set
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    

}