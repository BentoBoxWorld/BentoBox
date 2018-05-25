package us.tastybento.bskyblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import us.tastybento.bskyblock.Constants.GameType;
import us.tastybento.bskyblock.api.configuration.ConfigComment;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.ISettings;
import us.tastybento.bskyblock.api.configuration.StoreAt;
import us.tastybento.bskyblock.api.configuration.WorldSettings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.BSBDbSetup.DatabaseType;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.PotionEffectListAdapter;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
public class Settings implements ISettings<Settings>, WorldSettings {

    private String uniqueId = "config";

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

    @ConfigEntry(path = "general.fakeplayers")
    private Set<String> fakePlayers = new HashSet<>();

    @ConfigEntry(path = "general.allow-obsidian-scooping")
    private boolean allowObsidianScooping = true;
    
    @ConfigComment("Time in seconds that players have to confirm sensitive commands, e.g. island reset")
    @ConfigEntry(path = "general.confirmation-time")
    private int confirmationTime = 20;

    // ---------------------------------------------

    /*      WORLD       */
    @ConfigEntry(path = "world.friendly-name", needsReset = true)
    private String friendlyName = "BSkyBlock";
    
    @ConfigEntry(path = "world.world-name", needsReset = true)
    private String worldName = "BSkyBlock-world";

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
    
    @ConfigEntry(path = "world.end.dragon-spawn")
    private boolean dragonSpawn = false;
    

    // ---------------------------------------------

    /*      ISLAND      */
    // Entities
    @ConfigEntry(path = "island.limits.entities")
    private Map<EntityType, Integer> entityLimits = new EnumMap<>(EntityType.class);
    @ConfigEntry(path = "island.limits.tile-entities")
    private Map<String, Integer> tileEntityLimits = new HashMap<>();

    
    @ConfigEntry(path = "island.max-team-size")
    private int maxTeamSize = 4;
    @ConfigEntry(path = "island.max-homes")
    private int maxHomes = 5;
    @ConfigEntry(path = "island.name.min-length")
    private int nameMinLength = 4;
    @ConfigEntry(path = "island.name.max-length")
    private int nameMaxLength = 20;
    @ConfigEntry(path = "island.invite-wait")
    private int inviteWait = 60;

    // Reset
    @ConfigEntry(path = "island.reset.reset-limit")
    private int resetLimit = -1;

    @ConfigEntry(path = "island.require-confirmation.reset")
    private boolean resetConfirmation = true;

    @ConfigEntry(path = "island.reset-wait")
    private long resetWait = 10L;

    @ConfigEntry(path = "island.reset.leavers-lose-reset")
    private boolean leaversLoseReset = false;

    @ConfigEntry(path = "island.reset.kicked-keep-inventory")
    private boolean kickedKeepInventory = false;

    // Remove mobs
    @ConfigEntry(path = "island.remove-mobs.on-login")
    private boolean removeMobsOnLogin = false;
    @ConfigEntry(path = "island.remove-mobs.on-island")
    private boolean removeMobsOnIsland = false;

    @ConfigEntry(path = "island.remove-mobs.whitelist")
    private List<String> removeMobsWhitelist = new ArrayList<>();

    @ConfigEntry(path = "island.make-island-if-none")
    private boolean makeIslandIfNone = false;

    @ConfigEntry(path = "island.immediate-teleport-on-island")
    private boolean immediateTeleportOnIsland = false;

    private boolean respawnOnIsland = true;

    // Deaths
    @ConfigEntry(path = "island.deaths.max")
    private int deathsMax = 10;

    @ConfigEntry(path = "island.deaths.sum-team")
    private boolean deathsSumTeam = false;

    // Ranks
    @ConfigEntry(path = "island.customranks")
    private Map<String, Integer> customRanks = new HashMap<>();

    // ---------------------------------------------

    /*      PROTECTION      */
    @ConfigEntry(path = "protection.allow-piston-push")
    private boolean allowPistonPush = false;

    @ConfigEntry(path = "protection.restrict-flying-mobs")
    private boolean restrictFlyingMobs = true;

    private int togglePvPCooldown;

    private Map<Flag, Boolean> defaultFlags = new HashMap<>();

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
    @Adapter(PotionEffectListAdapter.class)
    private List<PotionEffectType> acidEffects = new ArrayList<>(Arrays.asList(PotionEffectType.CONFUSION, PotionEffectType.SLOW));

    /*      SCHEMATICS      */
    private List<String> companionNames = new ArrayList<>();

    @ConfigEntry(path = "island.chest-items")
    private List<ItemStack> chestItems = new ArrayList<>();

    private EntityType companionType = EntityType.COW;

    private boolean useOwnGenerator;

    private Map<String,Integer> limitedBlocks = new HashMap<>();
    private boolean teamJoinDeathReset;

    // Timeout for team kick and leave commands
    @ConfigEntry(path = "island.require-confirmation.kick")
    private boolean kickConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.kick-wait")
    private long kickWait = 10L;

    @ConfigEntry(path = "island.require-confirmation.leave")
    private boolean leaveConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.leave-wait")
    private long leaveWait = 10L;


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
     * @return the customRanks
     */
    public Map<String, Integer> getCustomRanks() {
        return customRanks;
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
    public Map<Flag, Boolean> getDefaultFlags() {
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
    public Map<EntityType, Integer> getEntityLimits() {
        return entityLimits;
    }
    @Override
    public Settings getInstance() {
        return this;
    }
    /**
     * Number of minutes to wait
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
    public Map<String, Integer> getLimitedBlocks() {
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
    public Map<String, Integer> getTileEntityLimits() {
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
    @Override
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
    @Override
    public boolean isNetherGenerate() {
        return netherGenerate;
    }
    /**
     * @return the netherIslands
     */
    @Override
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
    @Override
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
     * @param customRanks the customRanks to set
     */
    public void setCustomRanks(Map<String, Integer> customRanks) {
        this.customRanks = customRanks;
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
    public void setDefaultFlags(Map<Flag, Boolean> defaultFlags) {
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
    public void setEntityLimits(Map<EntityType, Integer> entityLimits) {
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
    public void setLimitedBlocks(Map<String, Integer> limitedBlocks) {
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
    public void setTileEntityLimits(Map<String, Integer> tileEntityLimits) {
        this.tileEntityLimits = tileEntityLimits;
    }
    /**
     * @param togglePvPCooldown the togglePvPCooldown to set
     */
    public void setTogglePvPCooldown(int togglePvPCooldown) {
        this.togglePvPCooldown = togglePvPCooldown;
    }
    /**
     * @param uniqueId - unique ID the uniqueId to set
     */
    @Override
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
    /**
     * @return the fakePlayers
     */
    public Set<String> getFakePlayers() {
        return fakePlayers;
    }
    /**
     * @param fakePlayers the fakePlayers to set
     */
    public void setFakePlayers(Set<String> fakePlayers) {
        this.fakePlayers = fakePlayers;
    }
    /**
     * @return the confirmationTime
     */
    public int getConfirmationTime() {
        return confirmationTime;
    }
    /**
     * @param confirmationTime the confirmationTime to set
     */
    public void setConfirmationTime(int confirmationTime) {
        this.confirmationTime = confirmationTime;
    }
    
    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.configuration.WorldSettings#getFriendlyName()
     */
    @Override
    public String getFriendlyName() {
        return friendlyName;
    }
    
    /**
     * @param friendlyName the friendlyName to set
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    /**
     * @return the dragonSpawn
     */
    public boolean isDragonSpawn() {
        return dragonSpawn;
    }
    /**
     * @param dragonSpawn the dragonSpawn to set
     */
    public void setDragonSpawn(boolean dragonSpawn) {
        this.dragonSpawn = dragonSpawn;
    }


}