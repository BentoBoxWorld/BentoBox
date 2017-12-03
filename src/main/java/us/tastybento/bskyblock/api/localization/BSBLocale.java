package us.tastybento.bskyblock.api.localization;

import org.bukkit.ChatColor;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.LocaleManager;
import us.tastybento.bskyblock.util.YamlResourceBundle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

public class BSBLocale {

    private BSkyBlock plugin = BSkyBlock.getPlugin();

    private Locale locale;
    private ResourceBundle resourceBundle;

    /**
     * Loads the locale into a ResourceBundle.
     * Locale files are .yml and have the filename "[identifier]_[country and language tag].yml", e.g. bsb_en_GB.yml or levels_fr_FR.yml
     *
     * @param identifier
     * @param languageTag
     *
     * @throws MalformedURLException
     */
    public BSBLocale(String identifier, String languageTag) throws MalformedURLException {
        this.locale = Locale.forLanguageTag(languageTag);
        File localeDir = new File(plugin.getDataFolder(), LocaleManager.LOCALE_FOLDER);
        this.resourceBundle = ResourceBundle.getBundle(identifier, locale, new URLClassLoader(new URL[] {localeDir.toURI().toURL()}), YamlResourceBundle.Control.INSTANCE);
    }

    /**
     * Get text from the yml file for this locale
     *
     * @param reference - the YAML node where the text is
     *
     * @return Text for this locale reference or the reference if nothing has been found
     */
    public String get(String reference) {
        if (resourceBundle.containsKey(reference)) {
            return ChatColor.translateAlternateColorCodes('&', resourceBundle.getString(reference));
        }
        return reference; // Return reference in case nothing has been found
    }

    /**
     * Returns the locale language
     * @return the locale language
     */
    public String getLanguageName(){
        if(locale == null) return "unknown";
        return locale.getDisplayLanguage(locale);
    }

    /**
     * Returns the locale country
     * @return the locale country
     */
    public String getCountryName(){
        if(locale == null) return "unknown";
        return locale.getDisplayCountry(locale);
    }

    /**
     * Returns the locale language tag (e.g: en-GB)
     * @return the locale language tag
     */
    public String getLanguageTag(){
        return locale.toLanguageTag();
    }
}
