package us.tastybento.bskyblock.api.localization;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author Poslovitch, Tastybento
 */
public class BSBLocale {

    private Locale locale;
    private YamlConfiguration config;
    private Map<String, String> cache;

    public BSBLocale(String languageTag, File file) {
        this.locale = Locale.forLanguageTag(languageTag);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.cache = new HashMap<>();
    }

    /**
     * Get text from the yml file for this locale
     * @param reference - the YAML node where the text is
     * @return Text for this locale reference or the reference if nothing has been found
     */
    public String get(String reference) {
        if (cache.containsKey(reference)) {
            return cache.get(reference);
        } else if (config.contains(reference)) {
            cache.put(reference, ChatColor.translateAlternateColorCodes('&', config.getString(reference)));
            return cache.get(reference);
        }
        return reference; // return reference in case nothing has been found
    }

    public void clearCache() {
        this.cache.clear();
    }

    /**
     * Returns the locale language
     * @return the locale language
     */
    public String getLanguage(){
        if(locale == null) return "unknown";

        return locale.getDisplayLanguage();
    }

    /**
     * Returns the locale country
     * @return the locale country
     */
    public String getCountry(){
        if(locale == null) return "unknown";

        return locale.getDisplayCountry();
    }

    /**
     * Returns the locale language tag (e.g: en-GB)
     * @return the locale language tag
     */
    public String toLanguageTag(){
        return this.locale.toLanguageTag();
    }

}
