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

    public LocalesManager(BentoBox plugin) {
        this.plugin = plugin;
        loadLocales("BentoBox"); // Default
    }

    /**
     * Gets the reference from the locale file for this user
     * @param user - the User
     * @param reference - a reference that can be found in a locale file
     * @return the translated string, or if the translation does not exist, the default language version, or if that does not exist null
     */
    public String get(User user, String reference) {
        BentoBoxLocale locale = languages.get(user.getLocale());
        if (locale != null && locale.contains(reference)) {
            return locale.get(reference);
        }
        // Return the default
        if (languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).contains(reference)) {
            return languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).get(reference);
        }
        // Or try in the en-US locale
        if (languages.get(Locale.forLanguageTag("en-US")).contains(reference)) {
            return languages.get(Locale.forLanguageTag("en-US")).get(reference);
        }
        return null;
    }

    /**
     * Loads all the locales available. If the locale folder does not exist, one will be created and
     * filled with locale files from the jar.
     */
    public void loadLocales(String parent) {
        // Describe the filter - we only want files that are correctly named
        // Files must be 9 chars long
        FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml") && name.length() == 9;

        // Run through the files and store the locales
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + parent);
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

        // Store all the locales available
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

    public Map<Locale, BentoBoxLocale> getLanguages() {
        return this.languages;
    }
}
