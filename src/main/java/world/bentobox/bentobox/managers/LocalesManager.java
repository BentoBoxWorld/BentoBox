package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.FileLister;

/**
 * @author tastybento, Poslovitch
 */
public class LocalesManager {

    private BentoBox plugin;
    private Map<Locale, BentoBoxLocale> languages = new HashMap<>();
    private static final String LOCALE_FOLDER = "locales";
    private static final String BENTOBOX = "BentoBox";

    public LocalesManager(BentoBox plugin) {
        this.plugin = plugin;
        copyLocalesFromJar(BENTOBOX);
        loadLocalesFromFile(BENTOBOX); // Default
    }

    /**
     * Gets the translated String corresponding to the reference from the locale file for this user.
     * @param user the User
     * @param reference a reference that can be found in a locale file
     * @return the translated String from the User's locale or from the server's locale or from the en-US locale, or null.
     */
    public String get(User user, String reference) {
        // Make sure the user is not null
        if (user != null) {
            BentoBoxLocale locale = languages.get(user.getLocale());
            if (locale != null && locale.contains(reference)) {
                return locale.get(reference);
            }
        }
        // No translation could be gotten from the player's locale, trying more generic solutions
        return get(reference);
    }
    
    /**
     * Gets the translated String corresponding to the reference from the locale file for this user.
     * @param user the User
     * @param reference a reference that can be found in a locale file
     * @param default to return if the reference cannot be found anywhere
     * @return the translated String from the User's locale or from the server's locale or from the en-US locale, or null.
     */
    public String getOrDefault(User user, String reference, String defaultText) {
        // Make sure the user is not null
        if (user != null) {
            BentoBoxLocale locale = languages.get(user.getLocale());
            if (locale != null && locale.contains(reference)) {
                return locale.get(reference);
            }
        }
        // No translation could be gotten from the player's locale, trying more generic solutions
        return getOrDefault(reference, defaultText);
    }

    /**
     * Gets the translated String corresponding to the reference from the server's or the en-US locale file.
     * @param reference a reference that can be found in a locale file
     * @return the translated String from the server's locale or from the en-US locale, or null.
     */
    public String get(String reference) {
        // Get the translation from the server's locale
        if (languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).contains(reference)) {
            return languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).get(reference);
        }
        // Get the translation from the en-US locale
        if (languages.get(Locale.forLanguageTag("en-US")).contains(reference)) {
            return languages.get(Locale.forLanguageTag("en-US")).get(reference);
        }
        return null;
    }
    
    /**
     * Gets the translated String corresponding to the reference from the server's or the en-US locale file
     * or if it cannot be found anywhere, use the default text supplied.
     * @param reference a reference that can be found in a locale file
     * @param default text to return if the reference cannot be found anywhere
     * @return the translated String from the server's locale or from the en-US locale, or default.
     */
    public String getOrDefault(String reference, String defaultText) {
        String result = get(reference);
        return result == null ? defaultText : result;
    }

    /**
     * Copies all the locale files from the plugin jar to the filesystem.
     * Only done if the locale folder does not already exist.
     * @param folderName - the name of the destination folder
     */
    private void copyLocalesFromJar(String folderName) {
        // Run through the files and store the locales
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + folderName);
        // If the folder does not exist, then make it and fill with the locale files from the jar
        // If it does exist, then new files will NOT be written!
        if (!localeDir.exists()) {
            localeDir.mkdirs();
            FileLister lister = new FileLister(plugin);
            try {
                for (String name : lister.listJar(LOCALE_FOLDER)) {
                    // We cannot use Bukkit's saveResource, because we want it to go into a specific folder, so...
                    // Get the last part of the name
                    int lastIndex = name.lastIndexOf('/');
                    File targetFile = new File(localeDir, name.substring(lastIndex >= 0 ? lastIndex : 0, name.length()));
                    copyFile(name, targetFile);
                }
            } catch (IOException e) {
                plugin.logError("Could not copy locale files from jar " + e.getMessage());
            }
        }
    }

    /**
     * Loads all the locales available in the locale folder given. Used for loading all locales from plugin and addons
     *
     * @param localeFolder - locale folder location relative to the plugin's data folder
     */
    public void loadLocalesFromFile(String localeFolder) {
        // Filter for files of length 9 and ending with .yml
        FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml") && name.length() == 9;

        // Get the folder
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + localeFolder);
        if (!localeDir.exists()) {
            // If there is no locale folder, then return
            return;
        }
        // Run through the files and store the locales
        for (File language : Objects.requireNonNull(localeDir.listFiles(ymlFilter))) {
            Locale localeObject = Locale.forLanguageTag(language.getName().substring(0, language.getName().length() - 4));

            try {
                YamlConfiguration languageYaml = YamlConfiguration.loadConfiguration(language);

                if (languages.containsKey(localeObject)) {
                    // Merge into current language
                    languages.get(localeObject).merge(languageYaml);
                } else {
                    // New language
                    languages.put(localeObject, new BentoBoxLocale(localeObject, languageYaml));
                }
            } catch (Exception e) {
                BentoBox.getInstance().logError("Could not load '" + language.getName() + "' : " + e.getMessage()
                + " with the following cause '" + e.getCause() + "'." +
                " The file has likely an invalid YML format or has been made unreadable during the process."
                        );
            }
        }
    }

    private void copyFile(String name, File targetFile) {
        try (InputStream initialStream = plugin.getResource(name)) {
            if (!targetFile.exists()) {
                java.nio.file.Files.copy(initialStream, targetFile.toPath());
            }
        } catch (IOException e) {
            plugin.logError("Could not copy locale files from jar " + e.getMessage());
        }
    }

    /**
     * Gets a list of all the locales loaded
     * @param sort - if true, the locales will be sorted by language tag
     * @return list of locales
     */
    public List<Locale> getAvailableLocales(boolean sort) {
        if (sort) {
            List<Locale> locales = new LinkedList<>(languages.keySet());

            locales.sort((locale1, locale2) -> {
                if (locale1.toLanguageTag().equals(plugin.getSettings().getDefaultLanguage())) return -2;
                else if (locale1.toLanguageTag().startsWith("en")) return -1;
                else if (locale1.toLanguageTag().equals(locale2.toLanguageTag())) return 0;
                else return 1;
            });

            return locales;
        } else {
            return new ArrayList<>(languages.keySet());
        }
    }

    /**
     * @return raw map of system locales to BentoBox locales
     */
    public Map<Locale, BentoBoxLocale> getLanguages() {
        return this.languages;
    }

    /**
     * Reloads all the language files from the filesystem
     */
    public void reloadLanguages() {
        languages.clear();
        copyLocalesFromJar(BENTOBOX);
        loadLocalesFromFile(BENTOBOX);
        plugin.getAddonsManager().getAddons().forEach(addon -> loadLocalesFromFile(addon.getDescription().getName()));
    }


}
