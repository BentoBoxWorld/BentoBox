package us.tastybento.bskyblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.ConfigEntry.GameType;
import us.tastybento.bskyblock.api.configuration.ISettings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.config.StoreAt;
import us.tastybento.bskyblock.database.BSBDatabase.DatabaseType;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
public class Settings2 implements ISettings<Settings2> {
        
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

    @ConfigEntry(path = "world.sea-height", specificTo = GameType.ACIDISLAND)
    private int seaHeight = 100;

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
    private int resetWait;
    private boolean leaversLoseReset;
    private boolean kickedKeepInventory;

    // Remove mobs
    private boolean removeMobsOnLogin;
    private boolean removeMobsOnIsland;
    private List<String> removeMobsWhitelist;

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

    @ConfigEntry(path = "acid.damage.effects", specificTo = GameType.ACIDISLAND)
    private List<PotionEffectType> acidEffects = new ArrayList<>(Arrays.asList(PotionEffectType.CONFUSION, PotionEffectType.SLOW));

    /*      SCHEMATICS      */
    private List<String> companionNames;
    private ItemStack[] chestItems;
    private EntityType companionType;

    private boolean useOwnGenerator;

    private HashMap<String,Integer> limitedBlocks;
    private boolean teamJoinDeathReset;
    
    private String uniqueId;

    /**
     * @return the uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }
    /**
     * @param uniqueId the uniqueId to set
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    /**
     * @return the checkUpdates
     */
    public boolean isCheckUpdates() {
        return checkUpdates;
    }
    /**
     * @param checkUpdates the checkUpdates to set
     */
    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
    }
    /**
     * @return the defaultLanguage
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * @param defaultLanguage the defaultLanguage to set
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    /**
     * @return the useEconomy
     */
    public boolean isUseEconomy() {
        return useEconomy;
    }
    /**
     * @param useEconomy the useEconomy to set
     */
    public void setUseEconomy(boolean useEconomy) {
        this.useEconomy = useEconomy;
    }
    /**
     * @return the purgeMaxIslandLevel
     */
    public int getPurgeMaxIslandLevel() {
        return purgeMaxIslandLevel;
    }
    /**
     * @param purgeMaxIslandLevel the purgeMaxIslandLevel to set
     */
    public void setPurgeMaxIslandLevel(int purgeMaxIslandLevel) {
        this.purgeMaxIslandLevel = purgeMaxIslandLevel;
    }
    /**
     * @return the purgeRemoveUserData
     */
    public boolean isPurgeRemoveUserData() {
        return purgeRemoveUserData;
    }
    /**
     * @param purgeRemoveUserData the purgeRemoveUserData to set
     */
    public void setPurgeRemoveUserData(boolean purgeRemoveUserData) {
        this.purgeRemoveUserData = purgeRemoveUserData;
    }
    /**
     * @return the databaseType
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    /**
     * @param databaseType the databaseType to set
     */
    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }
    /**
     * @return the dbHost
     */
    public String getDbHost() {
        return dbHost;
    }
    /**
     * @param dbHost the dbHost to set
     */
    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }
    /**
     * @return the dbPort
     */
    public int getDbPort() {
        return dbPort;
    }
    /**
     * @param dbPort the dbPort to set
     */
    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }
    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }
    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    /**
     * @return the dbUsername
     */
    public String getDbUsername() {
        return dbUsername;
    }
    /**
     * @param dbUsername the dbUsername to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }
    /**
     * @return the dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }
    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
    /**
     * @return the databaseBackupPeriod
     */
    public int getDatabaseBackupPeriod() {
        return databaseBackupPeriod;
    }
    /**
     * @param databaseBackupPeriod the databaseBackupPeriod to set
     */
    public void setDatabaseBackupPeriod(int databaseBackupPeriod) {
        this.databaseBackupPeriod = databaseBackupPeriod;
    }
    /**
     * @return the allowAutoActivator
     */
    public boolean isAllowAutoActivator() {
        return allowAutoActivator;
    }
    /**
     * @param allowAutoActivator the allowAutoActivator to set
     */
    public void setAllowAutoActivator(boolean allowAutoActivator) {
        this.allowAutoActivator = allowAutoActivator;
    }
    /**
     * @return the allowObsidianScooping
     */
    public boolean isAllowObsidianScooping() {
        return allowObsidianScooping;
    }
    /**
     * @param allowObsidianScooping the allowObsidianScooping to set
     */
    public void setAllowObsidianScooping(boolean allowObsidianScooping) {
        this.allowObsidianScooping = allowObsidianScooping;
    }
    /**
     * @return the worldName
     */
    public String getWorldName() {
        return worldName;
    }
    /**
     * @param worldName the worldName to set
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    /**
     * @return the islandDistance
     */
    public int getIslandDistance() {
        return islandDistance;
    }
    /**
     * @param islandDistance the islandDistance to set
     */
    public void setIslandDistance(int islandDistance) {
        this.islandDistance = islandDistance;
    }
    /**
     * @return the islandProtectionRange
     */
    public int getIslandProtectionRange() {
        return islandProtectionRange;
    }
    /**
     * @param islandProtectionRange the islandProtectionRange to set
     */
    public void setIslandProtectionRange(int islandProtectionRange) {
        this.islandProtectionRange = islandProtectionRange;
    }
    /**
     * @return the islandStartX
     */
    public int getIslandStartX() {
        return islandStartX;
    }
    /**
     * @param islandStartX the islandStartX to set
     */
    public void setIslandStartX(int islandStartX) {
        this.islandStartX = islandStartX;
    }
    /**
     * @return the islandStartZ
     */
    public int getIslandStartZ() {
        return islandStartZ;
    }
    /**
     * @param islandStartZ the islandStartZ to set
     */
    public void setIslandStartZ(int islandStartZ) {
        this.islandStartZ = islandStartZ;
    }
    /**
     * @return the islandXOffset
     */
    public int getIslandXOffset() {
        return islandXOffset;
    }
    /**
     * @param islandXOffset the islandXOffset to set
     */
    public void setIslandXOffset(int islandXOffset) {
        this.islandXOffset = islandXOffset;
    }
    /**
     * @return the islandZOffset
     */
    public int getIslandZOffset() {
        return islandZOffset;
    }
    /**
     * @param islandZOffset the islandZOffset to set
     */
    public void setIslandZOffset(int islandZOffset) {
        this.islandZOffset = islandZOffset;
    }
    /**
     * @return the seaHeight
     */
    public int getSeaHeight() {
        return seaHeight;
    }
    /**
     * @param seaHeight the seaHeight to set
     */
    public void setSeaHeight(int seaHeight) {
        this.seaHeight = seaHeight;
    }
    /**
     * @return the islandHeight
     */
    public int getIslandHeight() {
        return islandHeight;
    }
    /**
     * @param islandHeight the islandHeight to set
     */
    public void setIslandHeight(int islandHeight) {
        this.islandHeight = islandHeight;
    }
    /**
     * @return the maxIslands
     */
    public int getMaxIslands() {
        return maxIslands;
    }
    /**
     * @param maxIslands the maxIslands to set
     */
    public void setMaxIslands(int maxIslands) {
        this.maxIslands = maxIslands;
    }
    /**
     * @return the netherGenerate
     */
    public boolean isNetherGenerate() {
        return netherGenerate;
    }
    /**
     * @param netherGenerate the netherGenerate to set
     */
    public void setNetherGenerate(boolean netherGenerate) {
        this.netherGenerate = netherGenerate;
    }
    /**
     * @return the netherIslands
     */
    public boolean isNetherIslands() {
        return netherIslands;
    }
    /**
     * @param netherIslands the netherIslands to set
     */
    public void setNetherIslands(boolean netherIslands) {
        this.netherIslands = netherIslands;
    }
    /**
     * @return the netherTrees
     */
    public boolean isNetherTrees() {
        return netherTrees;
    }
    /**
     * @param netherTrees the netherTrees to set
     */
    public void setNetherTrees(boolean netherTrees) {
        this.netherTrees = netherTrees;
    }
    /**
     * @return the netherRoof
     */
    public boolean isNetherRoof() {
        return netherRoof;
    }
    /**
     * @param netherRoof the netherRoof to set
     */
    public void setNetherRoof(boolean netherRoof) {
        this.netherRoof = netherRoof;
    }
    /**
     * @return the netherSpawnRadius
     */
    public int getNetherSpawnRadius() {
        return netherSpawnRadius;
    }
    /**
     * @param netherSpawnRadius the netherSpawnRadius to set
     */
    public void setNetherSpawnRadius(int netherSpawnRadius) {
        this.netherSpawnRadius = netherSpawnRadius;
    }
    /**
     * @return the endGenerate
     */
    public boolean isEndGenerate() {
        return endGenerate;
    }
    /**
     * @param endGenerate the endGenerate to set
     */
    public void setEndGenerate(boolean endGenerate) {
        this.endGenerate = endGenerate;
    }
    /**
     * @return the endIslands
     */
    public boolean isEndIslands() {
        return endIslands;
    }
    /**
     * @param endIslands the endIslands to set
     */
    public void setEndIslands(boolean endIslands) {
        this.endIslands = endIslands;
    }
    /**
     * @return the entityLimits
     */
    public HashMap<EntityType, Integer> getEntityLimits() {
        return entityLimits;
    }
    /**
     * @param entityLimits the entityLimits to set
     */
    public void setEntityLimits(HashMap<EntityType, Integer> entityLimits) {
        this.entityLimits = entityLimits;
    }
    /**
     * @return the tileEntityLimits
     */
    public HashMap<String, Integer> getTileEntityLimits() {
        return tileEntityLimits;
    }
    /**
     * @param tileEntityLimits the tileEntityLimits to set
     */
    public void setTileEntityLimits(HashMap<String, Integer> tileEntityLimits) {
        this.tileEntityLimits = tileEntityLimits;
    }
    /**
     * @return the maxTeamSize
     */
    public int getMaxTeamSize() {
        return maxTeamSize;
    }
    /**
     * @param maxTeamSize the maxTeamSize to set
     */
    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }
    /**
     * @return the maxHomes
     */
    public int getMaxHomes() {
        return maxHomes;
    }
    /**
     * @param maxHomes the maxHomes to set
     */
    public void setMaxHomes(int maxHomes) {
        this.maxHomes = maxHomes;
    }
    /**
     * @return the nameMinLength
     */
    public int getNameMinLength() {
        return nameMinLength;
    }
    /**
     * @param nameMinLength the nameMinLength to set
     */
    public void setNameMinLength(int nameMinLength) {
        this.nameMinLength = nameMinLength;
    }
    /**
     * @return the nameMaxLength
     */
    public int getNameMaxLength() {
        return nameMaxLength;
    }
    /**
     * @param nameMaxLength the nameMaxLength to set
     */
    public void setNameMaxLength(int nameMaxLength) {
        this.nameMaxLength = nameMaxLength;
    }
    /**
     * @return the inviteWait
     */
    public int getInviteWait() {
        return inviteWait;
    }
    /**
     * @param inviteWait the inviteWait to set
     */
    public void setInviteWait(int inviteWait) {
        this.inviteWait = inviteWait;
    }
    /**
     * @return the resetLimit
     */
    public int getResetLimit() {
        return resetLimit;
    }
    /**
     * @param resetLimit the resetLimit to set
     */
    public void setResetLimit(int resetLimit) {
        this.resetLimit = resetLimit;
    }
    /**
     * @return the resetWait
     */
    public int getResetWait() {
        return resetWait;
    }
    /**
     * @param resetWait the resetWait to set
     */
    public void setResetWait(int resetWait) {
        this.resetWait = resetWait;
    }
    /**
     * @return the leaversLoseReset
     */
    public boolean isLeaversLoseReset() {
        return leaversLoseReset;
    }
    /**
     * @param leaversLoseReset the leaversLoseReset to set
     */
    public void setLeaversLoseReset(boolean leaversLoseReset) {
        this.leaversLoseReset = leaversLoseReset;
    }
    /**
     * @return the kickedKeepInventory
     */
    public boolean isKickedKeepInventory() {
        return kickedKeepInventory;
    }
    /**
     * @param kickedKeepInventory the kickedKeepInventory to set
     */
    public void setKickedKeepInventory(boolean kickedKeepInventory) {
        this.kickedKeepInventory = kickedKeepInventory;
    }
    /**
     * @return the removeMobsOnLogin
     */
    public boolean isRemoveMobsOnLogin() {
        return removeMobsOnLogin;
    }
    /**
     * @param removeMobsOnLogin the removeMobsOnLogin to set
     */
    public void setRemoveMobsOnLogin(boolean removeMobsOnLogin) {
        this.removeMobsOnLogin = removeMobsOnLogin;
    }
    /**
     * @return the removeMobsOnIsland
     */
    public boolean isRemoveMobsOnIsland() {
        return removeMobsOnIsland;
    }
    /**
     * @param removeMobsOnIsland the removeMobsOnIsland to set
     */
    public void setRemoveMobsOnIsland(boolean removeMobsOnIsland) {
        this.removeMobsOnIsland = removeMobsOnIsland;
    }
    /**
     * @return the removeMobsWhitelist
     */
    public List<String> getRemoveMobsWhitelist() {
        return removeMobsWhitelist;
    }
    /**
     * @param removeMobsWhitelist the removeMobsWhitelist to set
     */
    public void setRemoveMobsWhitelist(List<String> removeMobsWhitelist) {
        this.removeMobsWhitelist = removeMobsWhitelist;
    }
    /**
     * @return the makeIslandIfNone
     */
    public boolean isMakeIslandIfNone() {
        return makeIslandIfNone;
    }
    /**
     * @param makeIslandIfNone the makeIslandIfNone to set
     */
    public void setMakeIslandIfNone(boolean makeIslandIfNone) {
        this.makeIslandIfNone = makeIslandIfNone;
    }
    /**
     * @return the immediateTeleportOnIsland
     */
    public boolean isImmediateTeleportOnIsland() {
        return immediateTeleportOnIsland;
    }
    /**
     * @param immediateTeleportOnIsland the immediateTeleportOnIsland to set
     */
    public void setImmediateTeleportOnIsland(boolean immediateTeleportOnIsland) {
        this.immediateTeleportOnIsland = immediateTeleportOnIsland;
    }
    /**
     * @return the respawnOnIsland
     */
    public boolean isRespawnOnIsland() {
        return respawnOnIsland;
    }
    /**
     * @param respawnOnIsland the respawnOnIsland to set
     */
    public void setRespawnOnIsland(boolean respawnOnIsland) {
        this.respawnOnIsland = respawnOnIsland;
    }
    /**
     * @return the deathsMax
     */
    public int getDeathsMax() {
        return deathsMax;
    }
    /**
     * @param deathsMax the deathsMax to set
     */
    public void setDeathsMax(int deathsMax) {
        this.deathsMax = deathsMax;
    }
    /**
     * @return the deathsSumTeam
     */
    public boolean isDeathsSumTeam() {
        return deathsSumTeam;
    }
    /**
     * @param deathsSumTeam the deathsSumTeam to set
     */
    public void setDeathsSumTeam(boolean deathsSumTeam) {
        this.deathsSumTeam = deathsSumTeam;
    }
    /**
     * @return the allowPistonPush
     */
    public boolean isAllowPistonPush() {
        return allowPistonPush;
    }
    /**
     * @param allowPistonPush the allowPistonPush to set
     */
    public void setAllowPistonPush(boolean allowPistonPush) {
        this.allowPistonPush = allowPistonPush;
    }
    /**
     * @return the restrictFlyingMobs
     */
    public boolean isRestrictFlyingMobs() {
        return restrictFlyingMobs;
    }
    /**
     * @param restrictFlyingMobs the restrictFlyingMobs to set
     */
    public void setRestrictFlyingMobs(boolean restrictFlyingMobs) {
        this.restrictFlyingMobs = restrictFlyingMobs;
    }
    /**
     * @return the togglePvPCooldown
     */
    public int getTogglePvPCooldown() {
        return togglePvPCooldown;
    }
    /**
     * @param togglePvPCooldown the togglePvPCooldown to set
     */
    public void setTogglePvPCooldown(int togglePvPCooldown) {
        this.togglePvPCooldown = togglePvPCooldown;
    }
    /**
     * @return the defaultFlags
     */
    public HashMap<Flag, Boolean> getDefaultFlags() {
        return defaultFlags;
    }
    /**
     * @param defaultFlags the defaultFlags to set
     */
    public void setDefaultFlags(HashMap<Flag, Boolean> defaultFlags) {
        this.defaultFlags = defaultFlags;
    }
    /**
     * @return the allowEndermanGriefing
     */
    public boolean isAllowEndermanGriefing() {
        return allowEndermanGriefing;
    }
    /**
     * @param allowEndermanGriefing the allowEndermanGriefing to set
     */
    public void setAllowEndermanGriefing(boolean allowEndermanGriefing) {
        this.allowEndermanGriefing = allowEndermanGriefing;
    }
    /**
     * @return the endermanDeathDrop
     */
    public boolean isEndermanDeathDrop() {
        return endermanDeathDrop;
    }
    /**
     * @param endermanDeathDrop the endermanDeathDrop to set
     */
    public void setEndermanDeathDrop(boolean endermanDeathDrop) {
        this.endermanDeathDrop = endermanDeathDrop;
    }
    /**
     * @return the allowTNTDamage
     */
    public boolean isAllowTNTDamage() {
        return allowTNTDamage;
    }
    /**
     * @param allowTNTDamage the allowTNTDamage to set
     */
    public void setAllowTNTDamage(boolean allowTNTDamage) {
        this.allowTNTDamage = allowTNTDamage;
    }
    /**
     * @return the allowChestDamage
     */
    public boolean isAllowChestDamage() {
        return allowChestDamage;
    }
    /**
     * @param allowChestDamage the allowChestDamage to set
     */
    public void setAllowChestDamage(boolean allowChestDamage) {
        this.allowChestDamage = allowChestDamage;
    }
    /**
     * @return the allowCreeperDamage
     */
    public boolean isAllowCreeperDamage() {
        return allowCreeperDamage;
    }
    /**
     * @param allowCreeperDamage the allowCreeperDamage to set
     */
    public void setAllowCreeperDamage(boolean allowCreeperDamage) {
        this.allowCreeperDamage = allowCreeperDamage;
    }
    /**
     * @return the allowCreeperGriefing
     */
    public boolean isAllowCreeperGriefing() {
        return allowCreeperGriefing;
    }
    /**
     * @param allowCreeperGriefing the allowCreeperGriefing to set
     */
    public void setAllowCreeperGriefing(boolean allowCreeperGriefing) {
        this.allowCreeperGriefing = allowCreeperGriefing;
    }
    /**
     * @return the allowMobDamageToItemFrames
     */
    public boolean isAllowMobDamageToItemFrames() {
        return allowMobDamageToItemFrames;
    }
    /**
     * @param allowMobDamageToItemFrames the allowMobDamageToItemFrames to set
     */
    public void setAllowMobDamageToItemFrames(boolean allowMobDamageToItemFrames) {
        this.allowMobDamageToItemFrames = allowMobDamageToItemFrames;
    }
    /**
     * @return the acidDamageOp
     */
    public boolean isAcidDamageOp() {
        return acidDamageOp;
    }
    /**
     * @param acidDamageOp the acidDamageOp to set
     */
    public void setAcidDamageOp(boolean acidDamageOp) {
        this.acidDamageOp = acidDamageOp;
    }
    /**
     * @return the acidDamageChickens
     */
    public boolean isAcidDamageChickens() {
        return acidDamageChickens;
    }
    /**
     * @param acidDamageChickens the acidDamageChickens to set
     */
    public void setAcidDamageChickens(boolean acidDamageChickens) {
        this.acidDamageChickens = acidDamageChickens;
    }
    /**
     * @return the acidDestroyItemTime
     */
    public int getAcidDestroyItemTime() {
        return acidDestroyItemTime;
    }
    /**
     * @param acidDestroyItemTime the acidDestroyItemTime to set
     */
    public void setAcidDestroyItemTime(int acidDestroyItemTime) {
        this.acidDestroyItemTime = acidDestroyItemTime;
    }
    /**
     * @return the acidDamage
     */
    public int getAcidDamage() {
        return acidDamage;
    }
    /**
     * @param acidDamage the acidDamage to set
     */
    public void setAcidDamage(int acidDamage) {
        this.acidDamage = acidDamage;
    }
    /**
     * @return the acidRainDamage
     */
    public int getAcidRainDamage() {
        return acidRainDamage;
    }
    /**
     * @param acidRainDamage the acidRainDamage to set
     */
    public void setAcidRainDamage(int acidRainDamage) {
        this.acidRainDamage = acidRainDamage;
    }
    /**
     * @return the acidEffects
     */
    public List<PotionEffectType> getAcidEffects() {
        return acidEffects;
    }
    /**
     * @param acidEffects the acidEffects to set
     */
    public void setAcidEffects(List<PotionEffectType> acidEffects) {
        this.acidEffects = acidEffects;
    }
    /**
     * @return the companionNames
     */
    public List<String> getCompanionNames() {
        return companionNames;
    }
    /**
     * @param companionNames the companionNames to set
     */
    public void setCompanionNames(List<String> companionNames) {
        this.companionNames = companionNames;
    }
    /**
     * @return the chestItems
     */
    public ItemStack[] getChestItems() {
        return chestItems;
    }
    /**
     * @param chestItems the chestItems to set
     */
    public void setChestItems(ItemStack[] chestItems) {
        this.chestItems = chestItems;
    }
    /**
     * @return the companionType
     */
    public EntityType getCompanionType() {
        return companionType;
    }
    /**
     * @param companionType the companionType to set
     */
    public void setCompanionType(EntityType companionType) {
        this.companionType = companionType;
    }
    /**
     * @return the useOwnGenerator
     */
    public boolean isUseOwnGenerator() {
        return useOwnGenerator;
    }
    /**
     * @param useOwnGenerator the useOwnGenerator to set
     */
    public void setUseOwnGenerator(boolean useOwnGenerator) {
        this.useOwnGenerator = useOwnGenerator;
    }
    /**
     * @return the limitedBlocks
     */
    public HashMap<String, Integer> getLimitedBlocks() {
        return limitedBlocks;
    }
    /**
     * @param limitedBlocks the limitedBlocks to set
     */
    public void setLimitedBlocks(HashMap<String, Integer> limitedBlocks) {
        this.limitedBlocks = limitedBlocks;
    }
    /**
     * @return the teamJoinDeathReset
     */
    public boolean isTeamJoinDeathReset() {
        return teamJoinDeathReset;
    }
    /**
     * @param teamJoinDeathReset the teamJoinDeathReset to set
     */
    public void setTeamJoinDeathReset(boolean teamJoinDeathReset) {
        this.teamJoinDeathReset = teamJoinDeathReset;
    }

}