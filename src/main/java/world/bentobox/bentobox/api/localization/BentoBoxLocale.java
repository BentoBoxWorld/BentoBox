package world.bentobox.bentobox.api.localization;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.util.ItemParser;

/**
 * @author Poslovitch, tastybento
 */
public class BentoBoxLocale {

    private static final String UNKNOWN = "unknown";

    private Locale locale;
    private YamlConfiguration config;
    private ItemStack banner;
    private List<String> authors;

    /**
     * List of available prefixes in this locale.
     * @since 1.12.0
     */
    private Set<String> prefixes;

    public BentoBoxLocale(Locale locale, YamlConfiguration config) {
        this.locale = locale;
        this.config = config;

        // Load the banner from the configuration
        banner = ItemParser.parse(config.getString("meta.banner"));

        // Load authors from the configuration
        authors = new LinkedList<>();
        updateAuthors(config);

        // Load prefixes from the configuration
        prefixes = new HashSet<>();
        updatePrefixes(config);
    }

    /**
     * Get text from the yml file for this locale
     * @param reference - the YAML node where the text is
     * @return Text for this locale reference or the reference if nothing has been found
     */
    public String get(String reference) {
        if (config.contains(reference)) {
            return config.getString(reference);
        }
        return reference; // return reference in case nothing has been found
    }

    /**
     * Returns the locale language
     * @return the locale language
     */
    public String getLanguage(){
        if(locale == null) {
            return UNKNOWN;
        }

        return locale.getDisplayLanguage();
    }

    /**
     * Returns the locale country
     * @return the locale country
     */
    public String getCountry(){
        if(locale == null) {
            return UNKNOWN;
        }

        return locale.getDisplayCountry();
    }

    /**
     * Returns the locale language tag (e.g: en-GB)
     * @return the locale language tag
     */
    public String toLanguageTag(){
        if(locale == null) {
            return UNKNOWN;
        }
        return locale.toLanguageTag();
    }

    /**
     * Returns the banner ItemStack representing this locale
     * @return the banner ItemStack
     */
    public ItemStack getBanner() {
        return banner;
    }

    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Merges a language YAML file to this locale
     * @param toBeMerged the YamlConfiguration of the language file
     */
    public void merge(@NonNull YamlConfiguration toBeMerged) {
        for (String key : toBeMerged.getKeys(true)) {
            if (!key.startsWith("meta") && !config.contains(key)) {
                config.set(key, toBeMerged.get(key));
            }
        }
        updateAuthors(toBeMerged);
        updatePrefixes(toBeMerged);
    }

    /**
     * Sets a reference and its value in the locale.
     * If the reference already exists, it will overwrite its value.
     * @param reference the reference to add, not null.
     * @param value the value to set, not null.
     * @since 1.6.0
     */
    public void set(@NonNull String reference, @NonNull String value) {
        config.set(reference, value);
    }

    public boolean contains(String reference) {
        return config.contains(reference);
    }

    private void updateAuthors(YamlConfiguration yamlConfiguration) {
        List<String> list = yamlConfiguration.getStringList("meta.authors");
        if (!list.isEmpty()) {
            for (String author : list) {
                if (!authors.contains(author)) {
                    authors.add(author);
                }
            }
        }
    }

    private void updatePrefixes(YamlConfiguration yamlConfiguration) {
        ConfigurationSection prefixesConfigSection = yamlConfiguration.getConfigurationSection("prefixes");
        if (prefixesConfigSection != null) {
            prefixes.addAll(prefixesConfigSection.getKeys(false));
        }
    }

    /**
     * @return the config
     */
    public YamlConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the list of prefixes available in this locale.
     * @return list of prefixes available in this locale.
     * @since 1.13.0
     */
    @NonNull
    public Set<String> getPrefixes() {
        return prefixes;
    }
}
