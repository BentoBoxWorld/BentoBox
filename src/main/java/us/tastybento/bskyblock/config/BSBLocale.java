package us.tastybento.bskyblock.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

public class BSBLocale {

    final static String LOCALE_FOLDER = "locales";
    private Plugin plugin;
    //private String localeId;
    private String languageTag;
    private ResourceBundle rb;
    Locale localeObject;

    /**
     * Provides localization
     * Locale files are .yml and have the filename "bsb_[country and language tag].yml", e.g. bsb_en_GB.yml
     * @param plugin
     * @throws MalformedURLException
     */
    public BSBLocale(Plugin plugin, String localeId) throws MalformedURLException {
        this.plugin = plugin;
        //this.localeId = localeId;
        // Check if the folder exists
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER);
        if (!localeDir.exists()) {
            localeDir.mkdirs();
        }
        // Check if this file does not exist
        File localeFile = new File(localeDir, localeId);
        if (!localeFile.exists()) {
            // Does not exist - look in JAR and save if possible
            plugin.saveResource(LOCALE_FOLDER + localeId, false);
        }
        languageTag = localeId.substring(4, localeId.length() - 4).replace('_', '-');
        URL[] urls = {localeDir.toURI().toURL()};
        ClassLoader loader = new URLClassLoader(urls);
        localeObject = Locale.forLanguageTag(languageTag);
        rb = ResourceBundle.getBundle("bsb", localeObject, loader, YamlResourceBundle.Control.INSTANCE);
    }

    /**
     * Get text from the yml file for this locale
     * @param reference - the YAML node where the text is
     * @return Text for this locale reference or the reference is nothing has been found
     */
    public String get(String reference) {
        // TODO: add placeholder conversion?
        //plugin.getLogger().info("DEBUG: default lang = " + Settings.defaultLanguage);
        //plugin.getLogger().info("DEBUG: this locale = " + languageTag);
        //plugin.getLogger().info("DEBUG: reference = " + reference);
        if (rb.containsKey(reference)) {
            //plugin.getLogger().info("DEBUG: contains key");
            return ChatColor.translateAlternateColorCodes('&', rb.getString(reference));
        } else if (!Settings.defaultLanguage.equals(languageTag)){
            //plugin.getLogger().info("DEBUG: try default");
            // TODO: Try default lang
            return reference;
        }
        plugin.getLogger().severe(reference + " not found in " + languageTag + " or default lang " + Settings.defaultLanguage);
        return reference; // Return reference for debug purposes, like for the mods.
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
    public String getLocaleId(){
        return this.localeObject.toLanguageTag();
    }

}
