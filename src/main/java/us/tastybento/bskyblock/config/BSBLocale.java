package us.tastybento.bskyblock.config;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Contains all the texts sent to players.
 * The locale object is instantiated at server launch, but the texts are only loaded when needed.
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
    private HashMap<String, String> localization = new HashMap<String, String>();
    
    public String get(String id){
        // If the text isn't loaded, load it.
        if(!localization.containsKey(id)){
            // Save the text to the HashMap.
            // If the text doesn't exist in the locale file, save it as its id, to help debug.
            localization.put(id, locale.getString(id, id));
        }
        return localization.get(id);
    }
}
