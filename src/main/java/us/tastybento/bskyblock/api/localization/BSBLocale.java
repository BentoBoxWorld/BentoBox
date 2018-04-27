package us.tastybento.bskyblock.api.localization;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

/**
 * @author Poslovitch, tastybento
 */
public class BSBLocale {

    private Locale locale;
    private YamlConfiguration config;
    private ItemStack banner;

    @SuppressWarnings("deprecation")
    public BSBLocale(Locale locale, File file) {
        this.locale = locale;
        config = YamlConfiguration.loadConfiguration(file);

        // Load the banner from the configuration
        List<String> bannerLayers = config.getStringList("banner");
        if (bannerLayers != null && !bannerLayers.isEmpty()) {
            banner = new ItemStack(Material.BANNER, 1);
            BannerMeta meta = (BannerMeta) banner.getItemMeta();

            meta.setBaseColor(DyeColor.valueOf(bannerLayers.get(0)));
            bannerLayers.remove(0);

            for (String s : bannerLayers) {
                String[] pattern = s.split(":");
                meta.addPattern(new Pattern(DyeColor.valueOf(pattern[1]), PatternType.valueOf(pattern[0])));
            }

            banner.setItemMeta(meta);
        } else {
            banner = new ItemStack(Material.BANNER, 1);
            BannerMeta meta = (BannerMeta) banner.getItemMeta();
            meta.setBaseColor(DyeColor.WHITE);
            banner.setItemMeta(meta);
        }
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
            return "unknown";
        }

        return locale.getDisplayLanguage();
    }

    /**
     * Returns the locale country
     * @return the locale country
     */
    public String getCountry(){
        if(locale == null) {
            return "unknown";
        }

        return locale.getDisplayCountry();
    }

    /**
     * Returns the locale language tag (e.g: en-GB)
     * @return the locale language tag
     */
    public String toLanguageTag(){
        return locale.toLanguageTag();
    }

    /**
     * Returns the banner ItemStack representing this locale
     * @return the banner ItemStack
     */
    public ItemStack getBanner() {
        return banner;
    }

    /**
     * Merges a language YAML file to this locale
     * @param language
     */
    public void merge(File language) {
        YamlConfiguration toBeMerged = YamlConfiguration.loadConfiguration(language);
        for (String key : toBeMerged.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, toBeMerged.get(key));
            }
        }
    }

    public boolean contains(String reference) {
        return config.contains(reference);
    }

}
