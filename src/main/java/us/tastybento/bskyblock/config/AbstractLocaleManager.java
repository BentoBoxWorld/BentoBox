package us.tastybento.bskyblock.config;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.util.FileLister;

/**
 * Handles locale functions
 * @author ben
 *
 */
public abstract class AbstractLocaleManager {
    private Plugin plugin;
    private HashMap<String, BSBLocale> locales = new HashMap<>();
    final static String LOCALE_FOLDER = "locales";
    
    public AbstractLocaleManager(Plugin plugin) {
        super();
        this.plugin = plugin;
        this.loadLocales();
    }

    /**
     * Returns an HashMap of locale identifier and the related object
     * @return the locales
     */
    public HashMap<String, BSBLocale> getLocales(){
        return locales;
    }

    /**
     * Set the available locales
     * @param locales - the locales to set
     */
    public void setLocales(HashMap<String, BSBLocale> locales){
        this.locales = locales;
    }

    /**
     * Returns the default locale
     * @return the default locale
     */
    public BSBLocale getLocale(){
        return locales.get(Settings.defaultLanguage);
    }

    /**
     * Returns the locale for the specified CommandSender
     * @param sender - CommandSender to get the locale
     * @return if sender is a player, the player's locale, otherwise the default locale
     */
    public BSBLocale getLocale(CommandSender sender){
        if(sender instanceof Player) return getLocale(((Player) sender).getUniqueId());
        else return getLocale();
    }

    /**
     * Returns the locale for the specified player
     * @param player - Player to get the locale
     * @return the locale for this player
     */
    public abstract BSBLocale getLocale(UUID player);

    /**
     * Loads all the locales available. If the locale folder does not exist, one will be created and
     * filled with locale files from the jar.
     */
    public void loadLocales() {
        // Describe the filter - we only want files that are correctly named
        FilenameFilter ymlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                //plugin.getLogger().info("DEBUG: filename = " + name);
                if (name.toLowerCase().startsWith("bsb_") && name.toLowerCase().endsWith(".yml")) {
                    // See if this is a valid locale
                    //Locale localeObject = new Locale(name.substring(0, 2), name.substring(3, 5));
                    Locale localeObject = Locale.forLanguageTag(name.substring(4, name.length() - 4));
                    if (localeObject == null) {
                        plugin.getLogger().severe("Filename '" + name + "' is an unknown locale, skipping...");
                        return false;
                    }
                    return true;
                } else {
                    if (name.toLowerCase().endsWith(".yml")) {
                        plugin.getLogger().severe("Filename '" + name + "' is not in the correct format for a locale file - skipping...");
                    }
                    return false;
                }
            }
        };
        // Run through the files and store the locales
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER);
        // If the folder does not exist, then make it and fill with the locale files from the jar
        if (!localeDir.exists()) {
            localeDir.mkdir();
            FileLister lister = new FileLister(plugin);
            try {
                for (String name : lister.listJar(LOCALE_FOLDER)) {
                    plugin.saveResource(name,true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Store all the locales available
        for (String language : localeDir.list(ymlFilter)) {
            try {
                BSBLocale locale = new BSBLocale(plugin, language);
                locales.put(locale.getLocaleId(), locale);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        }
    }
}
