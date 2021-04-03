package world.bentobox.bentobox.hooks;

import com.meowj.langutils.lang.LanguageHelper;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.LangUtil;

import java.util.Map.Entry;

/**
 * @author ApacheZy
 * @since 1.17
 */
@SuppressWarnings("unused")
public class LangUtilsHook extends Hook {

    public LangUtilsHook() {
        super("LangUtils", Material.BOOK);
    }

    @Override
    public boolean hook() {
        // Because there are other plugins with the same name,
        // we should check here whether it is the plugin we need.
        Plugin plugin = getPlugin();

        if (plugin != null && plugin.isEnabled()) {
            String tag = plugin.getConfig().getString("Extra-TAG");
            if ("tag_r72EhIAL".equals(tag)) {
                LangUtil.setHook();
                return true;
            }
        }

        return false;
    }

    @Override
    public String getFailureCause() {
        return "Unsupported version. Download the latest version: https://www.spigotmc.org/resources/langutils.89987/";
    }

    private String getUserLocale(User user) {
        return user.getLocale().toLanguageTag();
    }

    /**
     * Get the item display name.
     * <p>
     * If the item contains a custom name, return its custom name.
     * If the item itself does not have a custom name, the material
     * name will be translated and returned.
     *
     * @param item The item
     * @param user the User's locale will be used for translation.
     * @return The Display-Name of the item.
     */
    @NotNull
    public String getItemDisplayName(@NotNull ItemStack item, @NotNull User user) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return getItemName(item, user);
    }

    /**
     * Name translation of ItemStack.
     * <p>
     * Translate the material names of the items so that players can
     * see the names they know well.
     *
     * @param itemStack the ItemStack whose name will be translated.
     * @param user      the User's locale will be used for translation.
     * @return The translated item name.
     */
    @NotNull
    public String getItemName(@NotNull ItemStack itemStack, @NotNull User user) {
        return LanguageHelper.getItemName(itemStack, getUserLocale(user));
    }

    /**
     * Name translation of Bukkit material.
     * <p>
     * Translate the material names of the items so that players can
     * see the names they know well.
     *
     * @param material the Bukkit material whose name will be translated.
     * @param user     the User's locale will be used for translation.
     * @return The translated material name.
     */
    @NotNull
    public String getMaterialName(@NotNull Material material, @NotNull User user) {
        return LanguageHelper.getMaterialName(material, getUserLocale(user));
    }

    /**
     * Return the display name of the entity.
     *
     * @param entity The entity
     * @param user   the User's locale will be used for translation.
     * @return The name of the entity
     */
    @NotNull
    public String getEntityDisplayName(@NotNull Entity entity, @NotNull User user) {
        return entity.getCustomName() != null
            ? entity.getCustomName()
            : getEntityName(entity, user);
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entity the EntityType whose name will be translated.
     * @param user   the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NotNull
    public String getEntityName(@NotNull Entity entity, @NotNull User user) {
        return LanguageHelper.getEntityName(entity, getUserLocale(user));
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entityType the EntityType whose name will be translated.
     * @param user       the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    @NotNull
    public String getEntityName(@NotNull EntityType entityType, @NotNull User user) {
        return LanguageHelper.getEntityName(entityType, getUserLocale(user));
    }

    /**
     * Translate the name of the Biome.
     *
     * @param biome the Biome whose name will be translated.
     * @param user  the User's locale will be used for translation.
     * @return The translated Biome name.
     */
    @NotNull
    public String getBiomeName(@NotNull Biome biome, @NotNull User user) {
        return LanguageHelper.getBiomeName(biome, getUserLocale(user));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param enchant The enchantment.
     * @param level   The enchantment level.
     * @param user    The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    @NotNull
    public String getEnchantDisplayName(@NotNull Enchantment enchant, int level, @NotNull User user) {
        return LanguageHelper.getEnchantmentDisplayName(enchant, level, getUserLocale(user));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param entry The Entry of an enchantment with level The type
     *              is {@code Map.Entry<Enchantment, Integer>}
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    @NotNull
    public String getEnchantDisplayName(@NotNull Entry<Enchantment, Integer> entry, @NotNull User user) {
        return LanguageHelper.getEnchantmentDisplayName(entry, getUserLocale(user));
    }

    /**
     * Return the name of the enchantment.
     *
     * @param enchant The enchantment.
     * @param user    The User's locale will be used for translation.
     * @return The translated enchant name.
     */
    @NotNull
    public String getEnchantName(@NotNull Enchantment enchant, @NotNull User user) {
        return LanguageHelper.getEnchantmentName(enchant, getUserLocale(user));
    }

    /**
     * Return the enchantment level indicated by Roman numerals.
     * Can only get Roman numerals within 10.
     *
     * @param level The enchantment level.
     * @param user  The user's language will be used for translation.
     * @return The converted enchantment level.
     */
    @NotNull
    public String getEnchantLevelName(int level, @NotNull User user) {
        return LanguageHelper.getEnchantmentLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the name of the potion.
     *
     * @param potionType The type of the potion.
     * @param user       The user's language will be used for translation.
     * @return Translated potion name.
     */
    @NotNull
    public String getPotionTypeName(@NotNull PotionType potionType, @NotNull User user) {
        return LanguageHelper.getPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the splash potion.
     *
     * @param potionType The type of the splash potion.
     * @param user       The user's language will be used for translation.
     * @return Translated splash potion name.
     */
    @NotNull
    public String getSplashPotionName(@NotNull PotionType potionType, @NotNull User user) {
        return LanguageHelper.getSplashPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the lingering potion.
     *
     * @param potionType The type of lingering potion.
     * @param user       The user's language will be used for translation.
     * @return Translated lingering potion name.
     */
    @NotNull
    public String getLingeringPotionName(@NotNull PotionType potionType, @NotNull User user) {
        return LanguageHelper.getLingeringPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the tipped arrow.
     *
     * @param potionType Potion type of tipped arrow.
     * @param user       The user's language will be used for translation.
     * @return Translated tipped arrow name.
     */
    @NotNull
    public String getTippedArrowName(@NotNull PotionType potionType, @NotNull User user) {
        return LanguageHelper.getTippedArrowName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the base effect of the potion. If the PotionType
     * has no base effect, the translation of "No Effects" is returned. e.g.
     * Water Bottle, Mundane Potion.
     *
     * @param potionType The basic effect of PotionType.
     * @param user       The user's language will be used for translation.
     * @return Return the translation result.
     */
    @NotNull
    public String getPotionBaseEffectName(@NotNull PotionType potionType, @NotNull User user) {
        return LanguageHelper.getPotionBaseEffectName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion effect.
     *
     * @param effectType The potion effect.
     * @param user       The user's language will be used for translation.
     * @return Translated name of potion effect.
     */
    @NotNull
    public String getPotionEffectName(@NotNull PotionEffectType effectType, @NotNull User user) {
        return LanguageHelper.getPotionEffectName(effectType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion level.
     *
     * @param amplifier The  potion level.
     * @param user      The user's language will be used for translation.
     * @return The translated name of the potion level.
     */
    @NotNull
    public String getEffectAmplifierName(int amplifier, @NotNull User user) {
        return LanguageHelper.getEffectAmplifierName(amplifier, getUserLocale(user));
    }

    /**
     * Make the custom potion effect like the player usually sees.
     *
     * @param effect The potion effect.
     * @param user   The user's language will be used for translation.
     * @return The translated and formatted potion effect name, level, and duration.
     */
    @NotNull
    public String getPotionEffectDisplay(@NotNull PotionEffect effect, @NotNull User user) {
        return LanguageHelper.getPotionEffectDisplay(effect, getUserLocale(user));
    }

    /**
     * Translate the type name of tropical fish.
     *
     * @param fishPattern The type of tropical fish.
     * @param user        The user's language will be used for translation.
     * @return The translated name of the tropical fish type.
     */
    @NotNull
    public String getTropicalFishTypeName(@NotNull TropicalFish.Pattern fishPattern, @NotNull User user) {
        return LanguageHelper.getTropicalFishTypeName(fishPattern, getUserLocale(user));
    }

    /**
     * Get the names of 22 predefined tropical fish according to the
     * 'variant' tag of TropicalFish.
     *
     * @param meta Metadata carrying information about tropical fish.
     * @param user The return value is localized according to the
     *             user's locale.
     * @return If variant is predefined, return the name of the
     *     tropical fish, otherwise return null.
     */
    @Nullable
    public String getPredefinedTropicalFishName(@NotNull TropicalFishBucketMeta meta, @NotNull User user) {
        return LanguageHelper.getPredefinedTropicalFishName(meta, getUserLocale(user));
    }

    /**
     * Translate the name of the dye color.
     *
     * @param color The color of the dye.
     * @param user  The user's language will be used for translation.
     * @return The name of the dye color that has been translated.
     */
    @NotNull
    public String getDyeColorName(@NotNull DyeColor color, @NotNull User user) {
        return LanguageHelper.getDyeColorName(color, getUserLocale(user));
    }

    /**
     * Translate merchant's level name.
     *
     * @param level The merchant's level.
     * @param user  The user's language will be used for translation.
     * @return Translated name of merchant's level.
     */
    @NotNull
    public String getVillagerLevelName(int level, @NotNull User user) {
        return LanguageHelper.getVillagerLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the profession name of the villager.
     *
     * @param profession The villager's profession.
     * @param user       The user's language will be used for translation.
     * @return The translated profession name of the villager.
     */
    @NotNull
    public String getVillagerProfessionName(@NotNull Villager.Profession profession, @NotNull User user) {
        return LanguageHelper.getVillagerProfessionName(profession, getUserLocale(user));
    }

    /**
     * Translate the name of the banner pattern.
     *
     * @param pattern Contains the color banner pattern.
     * @param user    The user's language will be used for translation.
     * @return The translated name of banner pattern.
     */
    @NotNull
    public String getBannerPatternName(@NotNull Pattern pattern, @NotNull User user) {
        return LanguageHelper.getBannerPatternName(pattern, getUserLocale(user));
    }

    /**
     * Get the description of the music disk.
     *
     * @param material Material for music records.
     * @param user     The user's language will be used for translation.
     * @return If the given material is a music disk, the description of the
     *     music disk is returned. Otherwise, return NULL.
     */
    @Nullable
    public String getMusicDiskDesc(@NotNull Material material, @NotNull User user) {
        return LanguageHelper.getMusicDiskDesc(material, getUserLocale(user));
    }

    /**
     * Get the description of the Banner-Pattern added in Minecraft 1.14 and
     * above.
     *
     * @param material Material for banner pattern items.
     * @param user     The user's language will be used for translation.
     * @return The description of the Banner-Pattern item.
     */
    @Nullable
    public String getNewBannerPatternDesc(@NotNull Material material, @NotNull User user) {
        return LanguageHelper.getNewBannerPatternDesc(material, getUserLocale(user));
    }

}
