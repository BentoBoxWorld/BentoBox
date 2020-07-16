package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.FileLister;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento, Poslovitch
 */
public class LocalesManager {

    private BentoBox plugin;
    private Map<Locale, BentoBoxLocale> languages = new HashMap<>();
    private static final String LOCALE_FOLDER = "locales";
    private static final String BENTOBOX = "BentoBox";
    private static final String SPACER = "*************************************************";

    public LocalesManager(BentoBox plugin) {
        this.plugin = plugin;
        copyLocalesFromPluginJar();
        loadLocalesFromFile(BENTOBOX); // Default
    }

    /**
     * Gets the translated String corresponding to the reference from the locale file for this user.
     * @param user the User
     * @param reference a reference that can be found in a locale file
     * @return the translated String from the User's locale or from the server's locale or from the en-US locale, or null.
     */
    @Nullable
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
     * @param defaultText to return if the reference cannot be found anywhere
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
    @Nullable
    public String get(String reference) {
        // Get the translation from the server's locale
        if (languages.containsKey(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage()))
                && languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).contains(reference)) {
            return languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).get(reference);
        }
        // Get the translation from the en-US locale
        if (languages.get(Locale.forLanguageTag("en-US")).contains(reference)) {
            return languages.get(Locale.forLanguageTag("en-US")).get(reference);
        }
        return null;
    }

    /**
     * Gets the translated String corresponding to the reference from the server's or the en-US locale file.
     * or if it cannot be found anywhere, use the default text supplied.
     * @param reference a reference that can be found in a locale file.
     * @param defaultText text to return if the reference cannot be found anywhere.
     * @return the translated String from the server's locale or from the en-US locale, or default.
     */
    public String getOrDefault(String reference, String defaultText) {
        String result = get(reference);
        return result == null ? defaultText : result;
    }

    /**
     * Gets the list of prefixes from the user's locale, the server's locale and the en-US locale file.
     * @param user the user to get the locale, not null.
     * @return the list of prefixes from the user's locale, the server's locale and the en-US locale file.
     * @since 1.13.0
     */
    public Set<String> getAvailablePrefixes(@NonNull User user) {
        Set<String> prefixes = new HashSet<>();

        // Get the player locale
        BentoBoxLocale locale = languages.get(user.getLocale());
        if (locale != null) {
            prefixes.addAll(locale.getPrefixes());
        }

        // Get the prefixes from the server's locale
        prefixes.addAll(languages.get(Locale.forLanguageTag(plugin.getSettings().getDefaultLanguage())).getPrefixes());

        // Get the prefixes from the en-US locale
        prefixes.addAll(languages.get(Locale.forLanguageTag("en-US")).getPrefixes());

        return prefixes;
    }

    /**
     * Copies locale files from the addon jar to the file system and updates current locales with the latest references
     * @param addon - addon
     */
    void copyLocalesFromAddonJar(Addon addon) {
        try (JarFile jar = new JarFile(addon.getFile())) {
            File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + addon.getDescription().getName());
            if (!localeDir.exists()) {
                localeDir.mkdirs();
            }
            // Obtain any locale files, save them and update
            Util.listJarFiles(jar, LOCALE_FOLDER, ".yml").forEach(lf -> {
                File file = addon.saveResource(lf, localeDir, false, true);
                // Update
                if (file != null) {
                    updateLocale(addon, file, lf);
                }
            });

        } catch (Exception e) {
            plugin.logError(e.getMessage());
        }
    }

    private void updateLocale(Addon addon, File fileLocaleFile, String lf) {
        try {
            // Load the JAR locale file
            YamlConfiguration jarLocale = addon.getYamlFromJar(lf);
            // Load the locale file system locale file
            YamlConfiguration fileLocale = new YamlConfiguration();
            fileLocale.load(fileLocaleFile);
            // Copy new keys to file
            jarLocale.getKeys(true).stream().filter(k -> !fileLocale.contains(k, false)).forEach(k -> fileLocale.set(k, jarLocale.get(k)));
            // Save file
            fileLocale.save(fileLocaleFile);
        } catch (InvalidConfigurationException e) {
            plugin.logError("Could not update locale file '" + lf + "' due to it being malformed: " + e.getMessage());
        } catch (Exception e) {
            plugin.logError("Error updating locale file '" + lf + "': " + e.getMessage());
            plugin.logStacktrace(e);
        }
    }

    /**
     * Copies all the locale files from the plugin jar to the filesystem.
     * Only done if the locale folder does not already exist.
     */
    private void copyLocalesFromPluginJar() {
        // Run through the files and store the locales
        File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + LocalesManager.BENTOBOX);
        // If the folder does not exist, then make it and fill with the locale files from the jar
        // If it does exist, then new files will NOT be written!
        if (!localeDir.exists()) {
            localeDir.mkdirs();
        }
        FileLister lister = new FileLister(plugin);
        try {
            for (String name : lister.listJar(LOCALE_FOLDER)) {
                // We cannot use Bukkit's saveResource, because we want it to go into a specific folder, so...
                // Get the last part of the name
                int lastIndex = name.lastIndexOf('/');
                File targetFile = new File(localeDir, name.substring(Math.max(lastIndex, 0)));
                copyFile(name, targetFile);
                // Update the locale file if it exists already
                try (InputStreamReader in = new InputStreamReader(plugin.getResource(name))) {
                    YamlConfiguration jarLocale = new YamlConfiguration();
                    jarLocale.load(in);

                    YamlConfiguration fileLocale = new YamlConfiguration();
                    fileLocale.load(targetFile);
                    for (String k : jarLocale.getKeys(true)) {
                        if (!fileLocale.contains(k, false)) {
                            fileLocale.set(k, jarLocale.get(k));
                        }
                    }
                    // Save it
                    fileLocale.save(targetFile);
                } catch (InvalidConfigurationException e) {
                    plugin.logError("Could not update locale files from jar " + e.getMessage());
                }

            }
        } catch (IOException e) {
            plugin.logError("Could not copy locale files from jar " + e.getMessage());
        }
    }

    /**
     * Loads all the locales available in the locale folder given. Used for loading all locales from plugin and addons
     *
     * @param localeFolder - locale folder location relative to the plugin's data folder
     */
    public void loadLocalesFromFile(String localeFolder) {
        // Filter for files ending with .yml with a name whose length is >= 6 (xx.yml)
        FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml") && name.length() >= 6;

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
                plugin.logError("Could not load '" + language.getName() + "' : " + e.getMessage()
                + " with the following cause '" + e.getCause() + "'." +
                        " The file has likely an invalid YML format or has been made unreadable during the process.");
            }
        }
    }

    private void copyFile(String name, File targetFile) {
        try (InputStream initialStream = plugin.getResource(name)) {
            if (initialStream != null && !targetFile.exists()) {
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
     * Returns {@code true} if this locale is available, {@code false} otherwise.
     * @param locale the locale, not null. Consider using {@link Locale#forLanguageTag(String)} if needed.
     * @return {@code true} if this locale is available, {@code false} otherwise.
     * @since 1.14.0
     */
    public boolean isLocaleAvailable(@NonNull Locale locale) {
        return languages.containsKey(locale);
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
        copyLocalesFromPluginJar();
        loadLocalesFromFile(BENTOBOX);
        plugin.getAddonsManager().getAddons().forEach(addon -> {
            copyLocalesFromAddonJar(addon);
            loadLocalesFromFile(addon.getDescription().getName());
        });
    }

    /**
     * Loads all the locales available in the locale folder given.
     * Used for loading all locales from plugin and addons.
     *
     * @param fix whether or not locale files with missing translations should be fixed.
     *            Not currently supported.
     * @since 1.5.0
     */
    public void analyzeLocales(boolean fix) {
        languages.clear();

        User user = User.getInstance(Bukkit.getConsoleSender());

        user.sendRawMessage(ChatColor.AQUA + SPACER);
        plugin.log(ChatColor.AQUA + "Analyzing BentoBox locale files");
        user.sendRawMessage(ChatColor.AQUA + SPACER);
        loadLocalesFromFile(BENTOBOX);
        if (languages.containsKey(Locale.US)) {
            analyze(user);
        } else {
            user.sendRawMessage(ChatColor.RED + "No US English in BentoBox to use for analysis!");
        }
        user.sendRawMessage(ChatColor.AQUA + "Analyzing Addon locale files");
        plugin.getAddonsManager().getAddons().forEach(addon -> {
            user.sendRawMessage(ChatColor.AQUA + SPACER);
            user.sendRawMessage(ChatColor.AQUA + "Analyzing addon " + addon.getDescription().getName());
            user.sendRawMessage(ChatColor.AQUA + SPACER);
            languages.clear();
            loadLocalesFromFile(addon.getDescription().getName());
            if (languages.containsKey(Locale.US)) {
                analyze(user);
            } else {
                user.sendRawMessage(ChatColor.RED + "No US English to use for analysis!");
            }
        });
        reloadLanguages();
    }

    /**
     *
     * @param user - user
     * @since 1.5.0
     */
    private void analyze(User user) {
        user.sendRawMessage(ChatColor.GREEN + "The following locales are supported:");
        languages.forEach((k,v) -> user.sendRawMessage(ChatColor.GOLD + k.toLanguageTag() + " " + k.getDisplayLanguage() + " " + k.getDisplayCountry()));
        // Start with US English
        YamlConfiguration usConfig = languages.get(Locale.US).getConfig();
        // Fix config
        YamlConfiguration fixConfig = new YamlConfiguration();
        languages.values().stream().filter(l -> !l.toLanguageTag().equals(Locale.US.toLanguageTag())).forEach(l -> {
            user.sendRawMessage(ChatColor.GREEN + SPACER);
            user.sendRawMessage(ChatColor.GREEN + "Analyzing locale file " + l.toLanguageTag() + ":");
            YamlConfiguration c = l.getConfig();
            boolean complete = true;
            for (String path : usConfig.getKeys(true)) {
                if (!c.contains(path, true)) {
                    complete = false;
                    fixConfig.set(path, user.getTranslationOrNothing(path).replace('§', '&'));
                }
            }
            if (complete) {
                user.sendRawMessage(ChatColor.GREEN + "Language file covers all strings.");
            } else {
                user.sendRawMessage(ChatColor.RED + "The following YAML is missing. Please translate it:");
                plugin.log("\n" + fixConfig.saveToString());
            }

        });
    }
}
