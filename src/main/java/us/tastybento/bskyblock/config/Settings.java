package us.tastybento.bskyblock.config;

import java.util.HashMap;
import java.util.List;

import us.tastybento.bskyblock.database.BSBDatabase.DatabaseType;
import us.tastybento.bskyblock.database.OfflineHistoryMessages.HistoryMessageType;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
public class Settings {
    
    public static final String PERMPREFIX = "askyblock.";
    
    public static String defaultLanguage;
    
    public static int backupPeriod;
    
    public static boolean useEconomy;
    public static boolean useMinishop;
    
    public static boolean useControlPanel;
    
    public static int defaultResetLimit;
    
    public static boolean metrics;
    
    public static DatabaseType databaseType;
    
    public static int minIslandNameLength;
    public static int maxIslandNameLength;
    
    public static HashMap<SettingsFlag, Boolean> defaultWorldSettings = new HashMap<SettingsFlag, Boolean>();
    public static HashMap<SettingsFlag, Boolean> defaultIslandSettings = new HashMap<SettingsFlag, Boolean>();
    public static HashMap<SettingsFlag, Boolean> defaultSpawnSettings = new HashMap<SettingsFlag, Boolean>();
    
    public static List<HistoryMessageType> historyMessagesTypes;

    public static int inviteWait;

    public static int maxDeaths;

    public static int islandDistance;
    
    public static int protectionRange;
}
