package us.tastybento.bskyblock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.configuration.ConfigComment;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.StoreAt;
import us.tastybento.bskyblock.api.configuration.WorldSettings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.BSBDbSetup.DatabaseType;
import us.tastybento.bskyblock.database.objects.DataObject;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.FlagSerializer;
import us.tastybento.bskyblock.database.objects.adapters.FlagSerializer2;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
public class Settings implements DataObject, WorldSettings {

    // ---------------------------------------------

    /*      GENERAL     */
    @ConfigComment("BSkyBlock uses bStats.org to get global data about the plugin to help improving it.")
    @ConfigComment("bStats has nearly no effect on your server's performance and the sent data is completely")
    @ConfigComment("anonymous so please consider twice if you really want to disable it.")
    @ConfigEntry(path = "general.metrics")
    private boolean metrics = true;

    @ConfigComment("Check for updates - this will tell Ops and the console if there is a new")
    @ConfigComment("version available. It contacts dev.bukkit.org to request the latest version")
    @ConfigComment("info. It does not download the latest version or change any files")
    @ConfigEntry(path = "general.check-updates")
    private boolean checkUpdates = true;

    @ConfigComment("Default language for new players.")
    @ConfigComment("This is the filename in the locale folder without .yml.")
    @ConfigComment("If this does not exist, the default en-US will be used.")
    @ConfigEntry(path = "general.default-language")
    private String defaultLanguage = "en-US";

    @ConfigComment("Use economy or not. If true, an economy plugin is required. If false, no money is used or give.")
    @ConfigComment("If there is no economy plugin present anyway, money will be automatically disabled.")
    @ConfigEntry(path = "general.use-economy")
    private boolean useEconomy = true;

    @ConfigComment("Starting money - this is how much money new players will have as their")
    @ConfigComment("balance at the start of an island.")
    @ConfigEntry(path = "general.starting-money")
    private double startingMoney = 10.0;

    // Purge
    @ConfigComment("Only islands below this level will be removed if they are abandoned and admins issue the purge command")
    @ConfigEntry(path = "general.purge.max-island-level")
    private int purgeMaxIslandLevel = 50;

    @ConfigComment("Remove user data when its island gets purged.")
    @ConfigComment("Helps a lot to avoid huge backups and can save some performance on startup,")
    @ConfigComment("but the player settings and data will be reset.")
    @ConfigEntry(path = "general.purge.remove-user-data")
    private boolean purgeRemoveUserData = false;

    // Database
    @ConfigComment("FLATFILE, MYSQL, MONGO")
    @ConfigComment("if you use MONGO, you must also run the BSBMongo plugin (not addon)")
    @ConfigComment("See https://github.com/tastybento/bsbMongo/releases/")
    @ConfigEntry(path = "general.database.type")
    private DatabaseType databaseType = DatabaseType.FLATFILE;

    @ConfigEntry(path = "general.database.host")
    private String dbHost = "localhost";

    @ConfigComment("Port 3306 is MySQL's default. Port 27017 is MongoDB's default.")
    @ConfigEntry(path = "general.database.port")
    private int dbPort = 3306;

    @ConfigEntry(path = "general.database.name")
    private String dbName = "BSkyBlock";

    @ConfigEntry(path = "general.database.username")
    private String dbUsername = "username";

    @ConfigEntry(path = "general.database.password")
    private String dbPassword = "password";

    @ConfigComment("How often the data will be saved to file in mins. Default is 5 minutes.")
    @ConfigComment("This helps prevent issues if the server crashes.")
    @ConfigComment("Data is also saved at important points in the game.")
    @ConfigEntry(path = "general.database.backup-period")
    private int databaseBackupPeriod = 5;

    @ConfigComment("Recover super flat - if the generator does not run for some reason, you can get")
    @ConfigComment("super flat chunks (grass). To remove automatically, select this option. Turn off")
    @ConfigComment("if there are no more because it may cause lag.")
    @ConfigComment("This will regenerate any chunks with bedrock at y=0 when they are loaded")
    @ConfigEntry(path = "general.recover-super-flat")
    private boolean recoverSuperFlat = false;

    @ConfigComment("Mute death messages")
    @ConfigEntry(path = "general.mute-death-messages")
    private boolean muteDeathMessages = false;

    @ConfigComment("Allow FTB Autonomous Activator to work (will allow a pseudo player [CoFH] to place and break blocks and hang items)")
    @ConfigComment("Add other fake player names here if required")
    @ConfigEntry(path = "general.fakeplayers")
    private Set<String> fakePlayers = new HashSet<>();

    @ConfigComment("Allow obsidian to be scooped up with an empty bucket back into lava")
    @ConfigComment("This only works if there is a single block of obsidian (no obsidian within 10 blocks)")
    @ConfigComment("Recommendation is to keep this true so that newbies don't bother you or reset their")
    @ConfigComment("island unnecessarily.")
    @ConfigEntry(path = "general.allow-obsidian-scooping")
    private boolean allowObsidianScooping = true;

    @ConfigComment("Time in seconds that players have to confirm sensitive commands, e.g. island reset")
    @ConfigEntry(path = "general.confirmation-time")
    private int confirmationTime = 20;

    // ---------------------------------------------

    /*      WORLD       */
    @ConfigComment("Friendly name for this world. Used in admin commands. Must be a single word")
    @ConfigEntry(path = "world.friendly-name", needsReset = true)
    private String friendlyName = "BSkyBlock";

    @ConfigComment("Name of the world - if it does not exist then it will be generated.")
    @ConfigComment("It acts like a prefix for nether and end (e.g. BSkyBlock, BSkyBlock_nether, BSkyBlock_end)")
    @ConfigEntry(path = "world.world-name", needsReset = true)
    private String worldName = "BSkyBlock-world";

    @ConfigComment("Radius of island in blocks. (So distance between islands is twice this)")
    @ConfigComment("Will be rounded up to the nearest 16 blocks.")
    @ConfigComment("It is the same for every dimension : Overworld, Nether and End.")
    @ConfigComment("This value cannot be changed mid-game and the plugin will not start if it is different.")
    @ConfigEntry(path = "world.distance-between-islands", needsReset = true)
    private int islandDistance = 200;

    @ConfigComment("Default protection range radius in blocks. Cannot be larger than distance.")
    @ConfigComment("Admins can change protection sizes for players individually using /bsadmin setrange")
    @ConfigComment("or set this permission: bskyblock.island.range.<number>")
    @ConfigEntry(path = "world.protection-range", overrideOnChange = true)
    private int islandProtectionRange = 100;

    @ConfigComment("Start islands at these coordinates. This is where new islands will start in the")
    @ConfigComment("world. These must be a factor of your island distance, but the plugin will auto")
    @ConfigComment("calculate the closest location on the grid. Islands develop around this location")
    @ConfigComment("both positively and negatively in a square grid.")
    @ConfigComment("If none of this makes sense, leave it at 0,0.")
    @ConfigEntry(path = "world.start-x", needsReset = true)
    private int islandStartX = 0;

    @ConfigEntry(path = "world.start-z", needsReset = true)
    private int islandStartZ = 0;

    @ConfigEntry(path = "world.offset-x")
    private int islandXOffset;
    @ConfigEntry(path = "world.offset-z")
    private int islandZOffset;

    @ConfigComment("Island height - Lowest is 5.")
    @ConfigComment("It is the y coordinate of the bedrock block in the schem")
    @ConfigEntry(path = "world.island-height")
    private int islandHeight = 100;

    @ConfigComment("Use your own world generator for this world. In this case, the plugin will not generate")
    @ConfigComment("anything.")
    @ConfigEntry(path = "world.use-own-generator")
    private boolean useOwnGenerator;

    @ConfigComment("Sea height (don't changes this mid-game unless you delete the world)")
    @ConfigComment("Minimum is 0, which means you are playing Skyblock!")
    @ConfigComment("If sea height is less than about 10, then players will drop right through it")
    @ConfigComment("if it exists. Makes for an interesting variation on skyblock.")
    @ConfigEntry(path = "world.sea-height")
    private int seaHeight = 0;

    @ConfigComment("Maximum number of islands in the world. Set to 0 for unlimited. ")
    @ConfigComment("If the number of islands is greater than this number, no new island will be created.")
    @ConfigEntry(path = "world.max-islands")
    private int maxIslands = -1;

    @ConfigComment("The default game mode for this world. Players will be set to this mode when they create")
    @ConfigComment("a new island for example. Options are SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR")
    @ConfigEntry(path = "world.default-game-mode")
    private GameMode defaultGameMode = GameMode.SURVIVAL;

    // Nether
    @ConfigComment("Generate Nether - if this is false, the nether world will not be made and access to")
    @ConfigComment("the nether will not occur. Other plugins may still enable portal usage.")
    @ConfigComment("Note: Some challenges will not be possible if there is no nether.")
    @ConfigComment("Note that with a standard nether all players arrive at the same portal and entering a")
    @ConfigComment("portal will return them back to their islands.")
    @ConfigEntry(path = "world.nether.generate")
    private boolean netherGenerate = true;

    @ConfigComment("Islands in Nether. Change to false for standard vanilla nether.")
    @ConfigEntry(path = "world.nether.islands", needsReset = true)
    private boolean netherIslands = true;

    @ConfigComment("Nether trees are made if a player grows a tree in the nether (gravel and glowstone)")
    @ConfigComment("Applies to both vanilla and islands Nether")
    @ConfigEntry(path = "world.nether.trees")
    private boolean netherTrees = true;

    @ConfigComment("Make the nether roof, if false, there is nothing up there")
    @ConfigComment("Change to false if lag is a problem from the generation")
    @ConfigComment("Only applies to islands Nether")
    @ConfigEntry(path = "world.nether.roof")
    private boolean netherRoof = true;

    @ConfigComment("Nether spawn protection radius - this is the distance around the nether spawn")
    @ConfigComment("that will be protected from player interaction (breaking blocks, pouring lava etc.)")
    @ConfigComment("Minimum is 0 (not recommended), maximum is 100. Default is 25.")
    @ConfigComment("Only applies to vanilla nether")
    @ConfigEntry(path = "world.nether.spawn-radius")
    private int netherSpawnRadius = 32;

    // End
    @ConfigEntry(path = "world.end.generate")
    private boolean endGenerate = true;

    @ConfigEntry(path = "world.end.islands", needsReset = true)
    private boolean endIslands = true;

    @ConfigEntry(path = "world.end.dragon-spawn")
    private boolean dragonSpawn = false;

    @ConfigComment("Removing mobs - this kills all monsters in the vicinity. Benefit is that it helps")
    @ConfigComment("players return to their island if the island has been overrun by monsters.")
    @ConfigComment("This setting is toggled in world flags and set by the settings GUI.")
    @ConfigComment("Mob white list - these mobs will NOT be removed when logging in or doing /island")
    @ConfigEntry(path = "world.remove-mobs-whitelist")
    private Set<EntityType> removeMobsWhitelist = new HashSet<>();

    @ConfigComment("World flags. These are boolean settings for various flags for this world")
    @ConfigEntry(path = "world.flags")
    private Map<String, Boolean> worldFlags = new HashMap<>();

    @ConfigComment("These are the default protection settings for new islands.")
    @ConfigComment("The value is the minimum island rank required allowed to do the action")
    @ConfigComment("Ranks are: Visitor = 0, Member = 900, Owner = 1000")
    @ConfigEntry(path = "world.default-island-flags")
    @Adapter(FlagSerializer.class)
    private Map<Flag, Integer> defaultIslandFlags = new HashMap<>();

    @ConfigComment("These are the default settings for new islands")
    @ConfigEntry(path = "world.default-island-settings")
    @Adapter(FlagSerializer2.class)
    private Map<Flag, Integer> defaultIslandSettings = new HashMap<>();

    @ConfigComment("These are the settings visible to users.")
    @ConfigEntry(path = "world.visible-settings")
    private List<String> visibleSettings = new ArrayList<>();

    // ---------------------------------------------

    /*      ISLAND      */
    // Entities
    @ConfigEntry(path = "island.limits.entities")
    private Map<EntityType, Integer> entityLimits = new EnumMap<>(EntityType.class);
    @ConfigEntry(path = "island.limits.tile-entities")
    private Map<String, Integer> tileEntityLimits = new HashMap<>();

    @ConfigComment("Default max team size")
    @ConfigComment("Use this permission to set for specific user groups: askyblock.team.maxsize.<number>")
    @ConfigComment("Permission size cannot be less than the default below. ")
    @ConfigEntry(path = "island.max-team-size")
    private int maxTeamSize = 4;
    @ConfigComment("Default maximum number of homes a player can have. Min = 1")
    @ConfigComment("Accessed via sethome <number> or go <number>")
    @ConfigComment("Use this permission to set for specific user groups: askyblock.island.maxhomes.<number>")
    @ConfigEntry(path = "island.max-homes")
    private int maxHomes = 5;
    @ConfigComment("Island naming")
    @ConfigComment("Only players with the TODO can name their island")
    @ConfigComment("It is displayed in the top ten and enter and exit announcements")
    @ConfigComment("It replaces the owner's name. Players can use & for color coding if they have the TODO permission")
    @ConfigComment("These set the minimum and maximum size of a name.")
    @ConfigEntry(path = "island.name.min-length")
    private int nameMinLength = 4;
    @ConfigEntry(path = "island.name.max-length")
    private int nameMaxLength = 20;
    @ConfigComment("How long a player must wait until they can rejoin a team island after being")
    @ConfigComment("kicked in minutes. This slows the effectiveness of players repeating challenges")
    @ConfigComment("by repetitively being invited to a team island.")
    @ConfigEntry(path = "island.invite-wait")
    private int inviteWait = 60;

    // Reset
    @ConfigComment("How many resets a player is allowed (override with /asadmin clearreset <player>)")
    @ConfigComment("Value of -1 means unlimited, 0 means hardcore - no resets.")
    @ConfigComment("Example, 2 resets means they get 2 resets or 3 islands lifetime")
    @ConfigEntry(path = "island.reset.reset-limit")
    private int resetLimit = -1;

    @ConfigEntry(path = "island.require-confirmation.reset")
    private boolean resetConfirmation = true;

    @ConfigComment("How long a player must wait before they can reset their island again in second")
    @ConfigEntry(path = "island.reset-wait")
    private long resetWait = 300;

    @ConfigComment("Kicked or leaving players lose resets")
    @ConfigComment("Players who leave a team will lose an island reset chance")
    @ConfigComment("If a player has zero resets left and leaves a team, they cannot make a new")
    @ConfigComment("island by themselves and can only join a team.")
    @ConfigComment("Leave this true to avoid players exploiting free islands")
    @ConfigEntry(path = "island.reset.leavers-lose-reset")
    private boolean leaversLoseReset = false;

    @ConfigComment("Allow kicked players to keep their inventory.")
    @ConfigComment("If false, kicked player's inventory will be thrown at the island leader if the")
    @ConfigComment("kicked player is online and in the island world.")
    @ConfigEntry(path = "island.reset.kicked-keep-inventory")
    private boolean kickedKeepInventory = false;

    @ConfigComment("What the plugin should reset when the player joins or creates an island")
    @ConfigComment("Reset Money - if this is true, will reset the player's money to the starting money")
    @ConfigComment("Recommendation is that this is set to true, but if you run multi-worlds")
    @ConfigComment("make sure your economy handles multi-worlds too.")
    @ConfigEntry(path = "island.reset.on-join.money")
    private boolean onJoinResetMoney = false;

    @ConfigComment("Reset inventory - if true, the player's inventory will be cleared.")
    @ConfigComment("Note: if you have MultiInv running or a similar inventory control plugin, that")
    @ConfigComment("plugin may still reset the inventory when the world changes.")
    @ConfigEntry(path = "island.reset.on-join.inventory")
    private boolean onJoinResetInventory = false;

    @ConfigComment("Reset Ender Chest - if true, the player's Ender Chest will be cleared.")
    @ConfigEntry(path = "island.reset.on-join.ender-chest")
    private boolean onJoinResetEnderChest = false;

    @ConfigComment("What the plugin should reset when the player leaves or is kicked from an island")
    @ConfigComment("Reset Money - if this is true, will reset the player's money to the starting money")
    @ConfigComment("Recommendation is that this is set to true, but if you run multi-worlds")
    @ConfigComment("make sure your economy handles multi-worlds too.")
    @ConfigEntry(path = "island.reset.on-leave.money")
    private boolean onLeaveResetMoney = false;

    @ConfigComment("Reset inventory - if true, the player's inventory will be cleared.")
    @ConfigComment("Note: if you have MultiInv running or a similar inventory control plugin, that")
    @ConfigComment("plugin may still reset the inventory when the world changes.")
    @ConfigEntry(path = "island.reset.on-leave.inventory")
    private boolean onLeaveResetInventory = false;

    @ConfigComment("Reset Ender Chest - if true, the player's Ender Chest will be cleared.")
    @ConfigEntry(path = "island.reset.on-leave.ender-chest")
    private boolean onLeaveResetEnderChest = false;

    @ConfigEntry(path = "island.make-island-if-none")
    private boolean makeIslandIfNone = false;
    @ConfigComment("Immediately teleport player to their island (home 1 if it exists) when entering the world")
    @ConfigEntry(path = "island.immediate-teleport-on-island")
    private boolean immediateTeleportOnIsland = false;
    @ConfigComment("Have player's respawn on their island if they die")
    @ConfigEntry(path = "island.respawn-on-island")
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
    @ConfigComment("Allow pistons to push outside of the protected area (maybe to make bridges)")
    @ConfigEntry(path = "protection.allow-piston-push")
    private boolean allowPistonPush = false;

    @ConfigComment("Restrict Wither and other flying mobs.")
    @ConfigComment("Any flying mobs that exit the island space where they were spawned will be removed.")
    @ConfigComment("Includes blaze and ghast. ")
    @ConfigEntry(path = "protection.restrict-flying-mobs")
    private boolean restrictFlyingMobs = true;

    private int togglePvPCooldown;

    //TODO transform these options below into flags
    private boolean allowEndermanGriefing;
    private boolean endermanDeathDrop;
    private boolean allowTNTDamage;
    private boolean allowChestDamage;
    private boolean allowCreeperDamage;
    private boolean allowCreeperGriefing;
    private boolean allowMobDamageToItemFrames;

    // Invincible visitor settings
    @ConfigComment("Invincible visitors. List of damages that will not affect visitors.")
    @ConfigComment("Make list blank if visitors should receive all damages")
    @ConfigEntry(path = "protection.invincible-visitors")
    private List<String> ivSettings = new ArrayList<>();

    // ---------------------------------------------


    /*      SCHEMATICS      */
    private List<String> companionNames = new ArrayList<>();

    @ConfigEntry(path = "island.chest-items")
    private List<ItemStack> chestItems = new ArrayList<>();

    private EntityType companionType = EntityType.COW;

    private Map<String,Integer> limitedBlocks = new HashMap<>();
    private boolean teamJoinDeathReset;

    // Timeout for team kick and leave commands
    @ConfigComment("Ask the player to confirm the command he is using by typing it again.")
    @ConfigComment("The 'wait' value is the number of seconds to wait for confirmation.")
    @ConfigEntry(path = "island.require-confirmation.kick")
    private boolean kickConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.kick-wait")
    private long kickWait = 10L;

    @ConfigEntry(path = "island.require-confirmation.leave")
    private boolean leaveConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.leave-wait")
    private long leaveWait = 10L;

    @ConfigEntry(path = "panel.close-on-click-outside")
    private boolean closePanelOnClickOutside = true;

    //---------------------------------------------------------------------------------------/
    private String uniqueId = "config";

    // Getters and setters

    /**
     * @return the metrics
     */
    public boolean isMetrics() {
        return metrics;
    }
    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }
    /**
     * @return the startingMoney
     */
    public double getStartingMoney() {
        return startingMoney;
    }
    /**
     * @param startingMoney the startingMoney to set
     */
    public void setStartingMoney(double startingMoney) {
        this.startingMoney = startingMoney;
    }
    /**
     * @return the recoverSuperFlat
     */
    public boolean isRecoverSuperFlat() {
        return recoverSuperFlat;
    }
    /**
     * @param recoverSuperFlat the recoverSuperFlat to set
     */
    public void setRecoverSuperFlat(boolean recoverSuperFlat) {
        this.recoverSuperFlat = recoverSuperFlat;
    }
    /**
     * @return the muteDeathMessages
     */
    public boolean isMuteDeathMessages() {
        return muteDeathMessages;
    }
    /**
     * @param muteDeathMessages the muteDeathMessages to set
     */
    public void setMuteDeathMessages(boolean muteDeathMessages) {
        this.muteDeathMessages = muteDeathMessages;
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
     * @return the defaultLanguage
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * @return the entityLimits
     */
    @Override
    public Map<EntityType, Integer> getEntityLimits() {
        return entityLimits;
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
    @Override
    public int getIslandDistance() {
        return islandDistance;
    }
    /**
     * @return the islandHeight
     */
    @Override
    public int getIslandHeight() {
        return islandHeight;
    }
    /**
     * @return the islandProtectionRange
     */
    @Override
    public int getIslandProtectionRange() {
        return islandProtectionRange;
    }
    /**
     * @return the islandStartX
     */
    @Override
    public int getIslandStartX() {
        return islandStartX;
    }
    /**
     * @return the islandStartZ
     */
    @Override
    public int getIslandStartZ() {
        return islandStartZ;
    }
    /**
     * @return the islandXOffset
     */
    @Override
    public int getIslandXOffset() {
        return islandXOffset;
    }
    /**
     * @return the islandZOffset
     */
    @Override
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
    @Override
    public int getMaxHomes() {
        return maxHomes;
    }
    /**
     * @return the maxIslands
     */
    @Override
    public int getMaxIslands() {
        return maxIslands;
    }
    /**
     * @return the maxTeamSize
     */
    @Override
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
    @Override
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
    @Override
    public int getSeaHeight() {
        return seaHeight;
    }
    /**
     * @return the tileEntityLimits
     */
    @Override
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
    @Override
    public String getWorldName() {
        return worldName;
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
    @Override
    public boolean isEndGenerate() {
        return endGenerate;
    }
    /**
     * @return the endIslands
     */
    @Override
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
    @Override
    public boolean isUseOwnGenerator() {
        return useOwnGenerator;
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
    @Override
    public boolean isDragonSpawn() {
        return dragonSpawn;
    }
    /**
     * @param dragonSpawn the dragonSpawn to set
     */
    public void setDragonSpawn(boolean dragonSpawn) {
        this.dragonSpawn = dragonSpawn;
    }

    @Override
    public String getPermissionPrefix() {
        return "bskyblock";
    }

    /**
     * Invincible visitor settings
     * @return the ivSettings
     */
    @Override
    public List<String> getIvSettings() {
        return ivSettings;
    }

    /**
     * @param ivSettings the ivSettings to set
     */
    public void setIvSettings(List<String> ivSettings) {
        this.ivSettings = ivSettings;
    }

    /**
     * @return the worldFlags
     */
    @Override
    public Map<String, Boolean> getWorldFlags() {
        return worldFlags;
    }
    /**
     * @param worldFlags the worldFlags to set
     */
    public void setWorldFlags(Map<String, Boolean> worldFlags) {
        this.worldFlags = worldFlags;
    }

    /**
     * @return whether panels should close when clicked outside or not
     */
    public boolean isClosePanelOnClickOutside() {
        return closePanelOnClickOutside;
    }

    /**
     * Set panel close on click outside
     * @param closePanelOnClickOutside - true means close panel when click is outside panel
     */
    public void setClosePanelOnClickOutside(boolean closePanelOnClickOutside) {
        this.closePanelOnClickOutside = closePanelOnClickOutside;
    }
    /**
     * @return the defaultGameMode
     */
    @Override
    public GameMode getDefaultGameMode() {
        return defaultGameMode;
    }
    /**
     * @param defaultGameMode the defaultGameMode to set
     */
    public void setDefaultGameMode(GameMode defaultGameMode) {
        this.defaultGameMode = defaultGameMode;
    }
    /**
     * @return the removeMobsWhitelist
     */
    @Override
    public Set<EntityType> getRemoveMobsWhitelist() {
        return removeMobsWhitelist;
    }
    /**
     * @param removeMobsWhitelist the removeMobsWhitelist to set
     */
    public void setRemoveMobsWhitelist(Set<EntityType> removeMobsWhitelist) {
        this.removeMobsWhitelist = removeMobsWhitelist;
    }
    /**
     * @return the onJoinResetMoney
     */
    @Override
    public boolean isOnJoinResetMoney() {
        return onJoinResetMoney;
    }
    /**
     * @return the onJoinResetInventory
     */
    @Override
    public boolean isOnJoinResetInventory() {
        return onJoinResetInventory;
    }
    /**
     * @return the onJoinResetEnderChest
     */
    @Override
    public boolean isOnJoinResetEnderChest() {
        return onJoinResetEnderChest;
    }
    /**
     * @return the onLeaveResetMoney
     */
    @Override
    public boolean isOnLeaveResetMoney() {
        return onLeaveResetMoney;
    }
    /**
     * @return the onLeaveResetInventory
     */
    @Override
    public boolean isOnLeaveResetInventory() {
        return onLeaveResetInventory;
    }
    /**
     * @return the onLeaveResetEnderChest
     */
    @Override
    public boolean isOnLeaveResetEnderChest() {
        return onLeaveResetEnderChest;
    }
    /**
     * @param onJoinResetMoney the onJoinResetMoney to set
     */
    public void setOnJoinResetMoney(boolean onJoinResetMoney) {
        this.onJoinResetMoney = onJoinResetMoney;
    }
    /**
     * @param onJoinResetInventory the onJoinResetInventory to set
     */
    public void setOnJoinResetInventory(boolean onJoinResetInventory) {
        this.onJoinResetInventory = onJoinResetInventory;
    }
    /**
     * @param onJoinResetEnderChest the onJoinResetEnderChest to set
     */
    public void setOnJoinResetEnderChest(boolean onJoinResetEnderChest) {
        this.onJoinResetEnderChest = onJoinResetEnderChest;
    }
    /**
     * @param onLeaveResetMoney the onLeaveResetMoney to set
     */
    public void setOnLeaveResetMoney(boolean onLeaveResetMoney) {
        this.onLeaveResetMoney = onLeaveResetMoney;
    }
    /**
     * @param onLeaveResetInventory the onLeaveResetInventory to set
     */
    public void setOnLeaveResetInventory(boolean onLeaveResetInventory) {
        this.onLeaveResetInventory = onLeaveResetInventory;
    }
    /**
     * @param onLeaveResetEnderChest the onLeaveResetEnderChest to set
     */
    public void setOnLeaveResetEnderChest(boolean onLeaveResetEnderChest) {
        this.onLeaveResetEnderChest = onLeaveResetEnderChest;
    }

    @Override
    public Optional<Addon> getAddon() {
        // This is a plugin, not an addon
        return Optional.empty();
    }
    /**
     * @return the defaultIslandProtection
     */
    @Override
    public Map<Flag, Integer> getDefaultIslandFlags() {
        return defaultIslandFlags;
    }
    /**
     * @return the visibleSettings
     */
    @Override
    public List<String> getVisibleSettings() {
        return visibleSettings;
    }
    /**
     */
    public void setDefaultIslandFlags(Map<Flag, Integer> defaultIslandFlags) {
        this.defaultIslandFlags = defaultIslandFlags;
    }
    /**
     * @param visibleSettings the visibleSettings to set
     */
    public void setVisibleSettings(List<String> visibleSettings) {
        this.visibleSettings = visibleSettings;
    }
    /**
     * @return the defaultIslandSettings
     */
    @Override
    public Map<Flag, Integer> getDefaultIslandSettings() {
        return defaultIslandSettings;
    }
    /**
     * @param defaultIslandSettings the defaultIslandSettings to set
     */
    public void setDefaultIslandSettings(Map<Flag, Integer> defaultIslandSettings) {
        this.defaultIslandSettings = defaultIslandSettings;
    }


}