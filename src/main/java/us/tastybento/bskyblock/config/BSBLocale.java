package us.tastybento.bskyblock.config;

import java.io.File;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Contains all the texts sent to players
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class BSBLocale {
    
    private BSkyBlock plugin;
    
    private String localeID;
    private FileConfiguration locale = null;
    private File localeFile = null;
    private Locale localeObject;
    
    /**
     * Creates a locale object full of localized string for a language
     * @param plugin
     * @param localeName - name of the yaml file that will be used
     */
    public BSBLocale(BSkyBlock plugin, String localeID){
        this.plugin = plugin;
        this.localeID = localeID;
        getLocale(localeID);
        loadLocale();
        
        localeObject = new Locale(localeID.substring(0, 2), localeID.substring(3, 5));
    }
    
    /**
     * Returns the locale language
     * @return the locale language
     */
    public String getLanguageName(){
        if(localeObject == null) return "unknown";
        
        return localeObject.getDisplayLanguage(localeObject);
    }
    
    /**
     * Returns the locale country
     * @return the locale country
     */
    public String getCountryName(){
        if(localeObject == null) return "unknown";
        
        return localeObject.getDisplayCountry(localeObject);
    }
    
    /**
     * Returns the locale identifier (e.g: en-GB)
     * @return the locale ID
     */
    public String getLocaleID(){
        return this.localeID;
    }
    
    /**
     * Returns the locale FileConfiguration
     * @param localeID - name of the yaml file to get
     * @return the FileConfiguration locale object
     */
    private FileConfiguration getLocale(String localeID){
        if(locale == null){
            reloadLocale(localeID);
        }
        return locale;
    }
    
    /**
     * Reloads the locale file
     * @param localeID - name of the yaml file to reload
     */
    private void reloadLocale(String localeID){
        // Make directory if it doesn't exist
        File directory = new File(plugin.getDataFolder() + File.separator + "locales");
        if(!directory.exists()) directory.mkdirs();
        
        if(localeFile == null) localeFile = new File(directory.getPath(), localeID + ".yml");
        
        if(localeFile.exists()){
            locale = YamlConfiguration.loadConfiguration(localeFile);
        } else {
            // Look for defaults in the jars
            if(plugin.getResource("locales/" + localeID + ".yml") != null){
                plugin.saveResource("locales/" + localeID + ".yml", true);
                localeFile = new File(directory.getPath(), localeID + ".yml");
                locale = YamlConfiguration.loadConfiguration(localeFile);
            } else {
                plugin.getLogger().severe("Could not find locale file '" + localeID + "' !");
            }
        }
    }
    
    /*      Localization        */
    
    // Not Setup
    public String notSetupHeader;
    public String notSetupDistance;
    public String notSetupGenerator;
    public String notSetupGeneratorMultiverse;
    public String notSetupWorldname;
    public String notSetupOutdated;
    
    // General
    public String generalSuccess;
    
    // Errors
    public String errorNoPermission;
    public String errorUseInGame;
    public String errorNoIsland;
    public String errorNotLeader;
    public String errorTooShort;
    public String errorTooLong;
    
    // Help
    public String helpSyntaxColor;
    public String helpCommentColor;
    public String helpHeader;
    public String islandHelpGo;
    public String islandHelpGoHomes;
    public String islandHelpSpawn;
    public String islandHelpCreate;
    public String islandHelpInfo;
    public String islandHelpControlPanel;
    public String islandHelpReset;
    public String islandHelpSetHome;
    public String islandHelpName;
    public String islandHelpResetName;
    public String islandHelpLimits;
    public String islandHelpTeam;
    public String islandHelpInvite;
    public String islandHelpUninvite;
    public String islandHelpLeave;
    public String islandHelpKick;
    public String islandHelpAccept;
    public String islandHelpReject;
    public String islandHelpMakeleader;
    public String islandHelpTeamchat;
    public String islandHelpBiomes;
    public String islandHelpExpel;
    public String islandHelpExpelall;
    public String islandHelpBan;
    public String islandHelpUnban;
    public String islandHelpBanlist;
    public String islandHelpTrust;
    public String islandHelpUntrust;
    public String islandHelpTrustlist;
    public String islandHelpCoop;
    public String islandHelpUncoop;
    public String islandHelpCooplist;
    public String islandHelpLock;
    public String islandHelpSettings;
    public String islandHelpLanguage;
    
    // Lock
    public String lockLocking;
    public String lockUnlocking;
    
    private void loadLocale(){
        // Not Setup
        notSetupHeader = ChatColor.translateAlternateColorCodes('&', locale.getString("not-setup.header", "More set up is required before the plugin can start...\nEdit config.yml. Then restart server."));
        notSetupDistance = ChatColor.translateAlternateColorCodes('&', locale.getString("not-setup.distance", "Make sure you set island distance. If upgrading, set it to what it was before."));
        notSetupGenerator = ChatColor.translateAlternateColorCodes('&', 
                locale.getString("not-setup.generator", "The world generator for the island world is not registered."
                        + "\nPotential reasons are:"
                        + "\n  1. If you are configuring the island world as the only server world\n     Make sure you have added the world to bukkit.yml"
                        + "\n  2. You reloaded instead of restarting the server. Reboot and try again."));
        notSetupGeneratorMultiverse = ChatColor.translateAlternateColorCodes('&', locale.getString("not-setup.generator-multiverse", "  3. Your Multiverse plugin is out of date. Upgrade to the latest version."));
        notSetupWorldname = ChatColor.translateAlternateColorCodes('&', 
                locale.getString("not-setup.world-name", "The world name in config.yml is different to the world name in islands.yml." 
                        + "\nIf this is intentional, we assume you are doing a full reset."
                        + "\nIf so, delete islands.yml and the previous world."
                        + "\nIf not, correct the world name in config.yml and restart. This is probably the case if you are upgrading."));
        notSetupOutdated = ChatColor.translateAlternateColorCodes('&', 
                locale.getString("not-setup.config-outdated", "The config.yml file looks outdated."
                        + "\nMake sure you updated your configuration after upgrading."
                        + "\nIf this error is still happening, you probably edited the old config rather than editing the new one."
                        + "\nIf so, please remove the current config.yml, work on config.new.yml and rename it to config.yml."));
        
        // General
        generalSuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("general.success", "Success!"));
        
        // Errors
        errorNoPermission = ChatColor.translateAlternateColorCodes('&', locale.getString("general.errors.no-permission", "You don't have permission to execute this command."));
    }
}
