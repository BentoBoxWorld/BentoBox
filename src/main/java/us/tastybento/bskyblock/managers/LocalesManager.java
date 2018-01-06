package us.tastybento.bskyblock.managers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Locale;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.localization.BSBLocale;
import us.tastybento.bskyblock.util.FileLister;

/**
 * @author Tastybento, Poslovitch
 */
public final class LocalesManager {

    private BSkyBlock plugin;
    private HashMap<Locale, BSBLocale> languages = new HashMap<>();
    final static String LOCALE_FOLDER = "locales";
    private static final boolean DEBUG = false;

    public LocalesManager(BSkyBlock plugin) {
        this.plugin = plugin;
        this.loadLocales(""); // Default
    }

    /**
     * Gets the reference from the locale file for this user
     * @param user
     * @param reference
     * @return the translated string, or if the translation does not exist, the default language version, or if that does not exist null
     */
    public String get(User user, String reference) {
        BSBLocale locale = languages.get(user.getLocale());
        if (locale != null && locale.contains(reference))
            return locale.get(reference);
        // Return the default
        if (languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).contains(reference)) {
            return languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).get(reference);
        }
        return null;
    }

    /**
     * Loads all the locales available. If the locale folder does not exist, one will be created and
     * filled with locale files from the jar.
     * TODO: Make more robust. The file filter is fragile.
     */
    public void loadLocales(String parent) {
        if (DEBUG) {
            if (parent.isEmpty())
                plugin.getLogger().info("DEBUG: loading locale for BSkyBlock");
            else
                plugin.getLogger().info("DEBUG: loading locale for " + parent);
        }
        // Describe the filter - we only want files that are correctly named
        FilenameFilter ymlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // Do BSkyBlock files
                if (parent.isEmpty()) {
                    if (name.toLowerCase().endsWith(".yml") && name.length() == 9) {               
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: bsb locale filename = " + name);
                        return true;
                    } 
                    return false;
                } else {
                    // Addon locales
                    if (name.startsWith(parent) && name.toLowerCase().endsWith(".yml")) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: addon locale filename = " + name);
                        return true;
                    }
                    return false;
                }
            }
        };

        // Run through the files and store the locales
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER);
        // If the folder does not exist, then make it and fill with the locale files from the jar
        // If it does exist, then new files will NOT be written!
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
        for (File language : localeDir.listFiles(ymlFilter)) {
            if (DEBUG) 
                plugin.getLogger().info("DEBUG: parent = " + parent + " language = " + language.getName().substring(parent.isEmpty() ? 0 : parent.length() + 1, language.getName().length() - 4));
            Locale localeObject = Locale.forLanguageTag(language.getName().substring(parent.isEmpty() ? 0 : parent.length() + 1, language.getName().length() - 4));
            if (DEBUG)
                plugin.getLogger().info("DEBUG: locale country found = " + localeObject.getCountry());
            if (languages.containsKey(localeObject)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: this locale is known");
                // Merge into current language
                languages.get(localeObject).merge(language);
            } else {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: this locale is not known - new language");
                // New language
                languages.put(localeObject, new BSBLocale(localeObject, language));
            }
        }
    }
}
