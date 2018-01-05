package us.tastybento.bskyblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.ISettings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.BSBDatabase.DatabaseType;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
public class Settings implements ISettings {
    // ----------------- Constants -----------------

    // Game Type BSKYBLOCK or ACIDISLAND
    public enum GameType {
        BSKYBLOCK, ACIDISLAND, BOTH
    }
    /*
    public final static GameType GAMETYPE = GameType.ACIDISLAND;
    // The spawn command (Essentials spawn for example)
    public final static String SPAWNCOMMAND = "spawn";
    // Permission prefix
    public final static String PERMPREFIX = "acidisland.";
    // The island command
    public final static String ISLANDCOMMAND = "ai";
    // The challenge command
    public static final String CHALLENGECOMMAND = "aic";
    // Admin command
    public static final String ADMINCOMMAND = "acid";
    */
    public final static GameType GAMETYPE = GameType.BSKYBLOCK;
    // Permission prefix
    public final static String PERMPREFIX = "bskyblock.";
    // The island command
    public final static String ISLANDCOMMAND = "island";
    // The challenge command
    public static final String CHALLENGECOMMAND = "bsc";
    // The spawn command (Essentials spawn for example)
    public final static String SPAWNCOMMAND = "spawn";
    // Admin command
    public static final String ADMINCOMMAND = "bsadmin";

    // ---------------------------------------------

    /*      GENERAL     */

    @ConfigEntry(path = "general.check-updates")
    public static boolean checkUpdates = true;

    @ConfigEntry(path = "general.default-language")
    public static String defaultLanguage = "en-US";

    @ConfigEntry(path = "general.use-economy")
    public static boolean useEconomy = true;

    // Purge
    @ConfigEntry(path = "general.purge.max-island-level")
    public static int purgeMaxIslandLevel = 50;

    @ConfigEntry(path = "general.purge.remove-user-data")
    public static boolean purgeRemoveUserData = false;

    // Database
    @ConfigEntry(path = "general.database.type", adapter = EnumAdapter.class)
    public static DatabaseType databaseType = DatabaseType.FLATFILE;

    @ConfigEntry(path = "general.database.host")
    public static String dbHost = "localhost";

    @ConfigEntry(path = "general.database.port")
    public static int dbPort = 3306;

    @ConfigEntry(path = "general.database.name")
    public static String dbName = "BSkyBlock";

    @ConfigEntry(path = "general.database.username")
    public static String dbUsername = "username";

    @ConfigEntry(path = "general.database.password")
    public static String dbPassword = "password";

    @ConfigEntry(path = "general.database.backup-period")
    public static int databaseBackupPeriod = 5;

    //TODO change allowAutoActivator to the fakePlayers introduced in ASB 3.0.8
    @ConfigEntry(path = "general.allow-FTB-auto-activators")
    public static boolean allowAutoActivator = false;

    @ConfigEntry(path = "general.allow-obsidian-scooping")
    public static boolean allowObsidianScooping = true;

    // ---------------------------------------------

    /*      WORLD       */
    @ConfigEntry(path = "world.world-name", needsReset = true)
    public static String worldName = "BSkyBlock";

    @ConfigEntry(path = "world.distance-between-islands", needsReset = true)
    public static int islandDistance = 200;

    @ConfigEntry(path = "world.protection-range", overrideOnChange = true)
    public static int islandProtectionRange = 100;

    @ConfigEntry(path = "world.start-x", needsReset = true)
    public static int islandStartX = 0;

    @ConfigEntry(path = "world.start-z", needsReset = true)
    public static int islandStartZ = 0;

    public static int islandXOffset;
    public static int islandZOffset;

    @ConfigEntry(path = "world.sea-height", specificTo = GameType.ACIDISLAND)
    public static int seaHeight = 100;

    @ConfigEntry(path = "world.island-height")
    public static int islandHeight = 100;

    @ConfigEntry(path = "world.max-islands")
    public static int maxIslands = -1;

    // Nether
    @ConfigEntry(path = "world.nether.generate")
    public static boolean netherGenerate = true;

    @ConfigEntry(path = "world.nether.islands", needsReset = true)
    public static boolean netherIslands = true;

    @ConfigEntry(path = "world.nether.trees")
    public static boolean netherTrees = true;

    @ConfigEntry(path = "world.nether.roof")
    public static boolean netherRoof = true;

    @ConfigEntry(path = "world.nether.spawn-radius")
    public static int netherSpawnRadius = 32;

    // End
    @ConfigEntry(path = "world.end.generate")
    public static boolean endGenerate = true;

    @ConfigEntry(path = "world.end.islands", needsReset = true)
    public static boolean endIslands = true;

    // Entities
    public static HashMap<EntityType, Integer> entityLimits;
    public static HashMap<String, Integer> tileEntityLimits;

    // ---------------------------------------------

    /*      ISLAND      */
    public static int maxTeamSize;
    public static int maxHomes;
    public static int nameMinLength;
    public static int nameMaxLength;
    public static int inviteWait;

    // Reset
    public static int resetLimit;
    public static int resetWait;
    public static boolean leaversLoseReset;
    public static boolean kickedKeepInventory;

    // Remove mobs
    public static boolean removeMobsOnLogin;
    public static boolean removeMobsOnIsland;
    public static List<String> removeMobsWhitelist;

    public static boolean makeIslandIfNone;
    public static boolean immediateTeleportOnIsland;
    public static boolean respawnOnIsland;

    // Deaths
    @ConfigEntry(path = "island.deaths.max")
    public static int deathsMax = 10;

    @ConfigEntry(path = "island.deaths.sum-team")
    public static boolean deathsSumTeam = false;

    // ---------------------------------------------

    /*      PROTECTION      */
    @ConfigEntry(path = "protection.allow-piston-push")
    public static boolean allowPistonPush = false;

    @ConfigEntry(path = "protection.restrict-flying-mobs")
    public static boolean restrictFlyingMobs = true;

    public static int togglePvPCooldown;

    public static HashMap<Flag, Boolean> defaultFlags;

    //TODO transform these options below into flags
    public static boolean allowEndermanGriefing;
    public static boolean endermanDeathDrop;
    public static boolean allowTNTDamage;
    public static boolean allowChestDamage;
    public static boolean allowCreeperDamage;
    public static boolean allowCreeperGriefing;
    public static boolean allowMobDamageToItemFrames;

    //TODO flags

    // ---------------------------------------------

    /*      ACID        */

    /*
     * This settings category only exists if the GameType is ACIDISLAND.
     */

    @ConfigEntry(path = "acid.damage-op", specificTo = GameType.ACIDISLAND)
    public static boolean acidDamageOp = false;

    @ConfigEntry(path = "acid.damage-chickens", specificTo = GameType.ACIDISLAND)
    public static boolean acidDamageChickens = false;

    @ConfigEntry(path = "acid.options.item-destroy-time", specificTo = GameType.ACIDISLAND)
    public static int acidDestroyItemTime = 0;

    // Damage
    @ConfigEntry(path = "acid.damage.acid.player", specificTo = GameType.ACIDISLAND)
    public static int acidDamage = 10;

    @ConfigEntry(path = "acid.damage.rain", specificTo = GameType.ACIDISLAND)
    public static int acidRainDamage = 1;

    @ConfigEntry(path = "acid.damage.effects", specificTo = GameType.ACIDISLAND)
    public static List<PotionEffectType> acidEffects = new ArrayList<>(Arrays.asList(PotionEffectType.CONFUSION, PotionEffectType.SLOW));

    /*      SCHEMATICS      */
    public static List<String> companionNames;
    public static ItemStack[] chestItems;
    public static EntityType companionType;

    public static boolean useOwnGenerator;

    public static HashMap<String,Integer> limitedBlocks;
    public static boolean teamJoinDeathReset;
}