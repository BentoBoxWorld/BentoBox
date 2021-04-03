package world.bentobox.bentobox.hooks;

import java.util.Map.Entry;

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
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import com.meowj.langutils.lang.LanguageHelper;

import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;

/**
 * @author ApacheZy
 * @since 1.17.0
 */
public class LangUtilsHook extends Hook {

    public LangUtilsHook() {
        super("LangUtils", Material.BOOK);
    }

    @Override
    public boolean hook() {
        return getPlugin() != null && getPlugin().isEnabled() && getPlugin().getConfig().getString("Extra-TAG", "").equals("tag_r72EhIAL");
    }

    @Override
    public String getFailureCause() {
        return "The LangUtils version does not apply to BentoBox. Download the latest version: https://github.com/apachezy/LangUtils/releases";
    }

    private String getUserLocale(User user) {
        return user.getLocale().toLanguageTag();
    }

    /**
     * Get the item display name.
     *
     * If the item contains a custom name, return its custom name.
     * If the item itself does not have a custom name, the material
     * name will be translated and returned.
     *
     * @param item The item
     * @param user the User's locale will be used for translation.
     * @return The Display-Name of the item.
     */
    public String getItemDisplayName(ItemStack item, User user) {
        return LanguageHelper.getItemDisplayName(item, getUserLocale(user));
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
    public String getItemName(ItemStack itemStack, User user) {
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
    public String getMaterialName(Material material, User user) {
        return LanguageHelper.getMaterialName(material, getUserLocale(user));
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entityType the EntityType whose name will be translated.
     * @param user       the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    public String getEntityName(EntityType entityType, User user) {
        return LanguageHelper.getEntityName(entityType, getUserLocale(user));
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entity the EntityType whose name will be translated.
     * @param user   the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    public String getEntityName(Entity entity, User user) {
        return LanguageHelper.getEntityName(entity, getUserLocale(user));
    }

    /**
     * Translate the name of the Biome.
     *
     * @param biome the Biome whose name will be translated.
     * @param user  the User's locale will be used for translation.
     * @return The translated Biome name.
     */
    public String getBiomeName(Biome biome, User user) {
        return LanguageHelper.getBiomeName(biome, getUserLocale(user));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param ench  The enchantment.
     * @param level The enchantment level.
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    public String getEnchantDisplayName(Enchantment ench, int level, User user) {
        return LanguageHelper.getEnchantmentDisplayName(ench, level, getUserLocale(user));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param entry The Entry of an enchantment with level The type
     *              is {@code Map.Entry<Enchantment, Integer>}
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    public String getEnchantDisplayName(Entry<Enchantment, Integer> entry, User user) {
        return LanguageHelper.getEnchantmentDisplayName(entry, getUserLocale(user));
    }

    /**
     * Return the name of the enchantment.
     *
     * @param enchant The enchantment.
     * @param user    The User's locale will be used for translation.
     * @return The translated enchant name.
     */
    public String getEnchantName(Enchantment enchant, User user) {
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
    public String getEnchantLevelName(int level, User user) {
        return LanguageHelper.getEnchantmentLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the name of the potion.
     *
     * @param potionType The type of the potion.
     * @param user       The user's language will be used for translation.
     * @return Translated potion name.
     */
    public String getPotionTypeName(PotionType potionType, User user) {
        return LanguageHelper.getPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the splash potion.
     *
     * @param potionType The type of the splash potion.
     * @param user       The user's language will be used for translation.
     * @return Translated splash potion name.
     */
    public String getSplashPotionName(PotionType potionType, User user) {
        return LanguageHelper.getSplashPotionName(potionType, getUserLocale(user));

    }

    /**
     * Translate the name of the lingering potion.
     *
     * @param potionType The type of lingering potion.
     * @param user       The user's language will be used for translation.
     * @return Translated lingering potion name.
     */
    public String getLingeringPotionName(PotionType potionType, User user) {
        return LanguageHelper.getLingeringPotionName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the tipped arrow.
     *
     * @param potionType Potion type of tipped arrow.
     * @param user       The user's language will be used for translation.
     * @return Translated tipped arrow name.
     */
    public String getTippedArrowName(PotionType potionType, User user) {
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
    public String getPotionBaseEffectName(PotionType potionType, User user) {
        return LanguageHelper.getPotionBaseEffectName(potionType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion effect.
     *
     * @param effectType The potion effect.
     * @param user       The user's language will be used for translation.
     * @return Translated name of potion effect.
     */
    public String getPotionEffectName(PotionEffectType effectType, User user) {
        return LanguageHelper.getPotionEffectName(effectType, getUserLocale(user));
    }

    /**
     * Translate the name of the potion level.
     *
     * @param amplifier The  potion level.
     * @param user      The user's language will be used for translation.
     * @return The translated name of the potion level.
     */
    public String getEffectAmplifierName(int amplifier, User user) {
        return LanguageHelper.getEffectAmplifierName(amplifier, getUserLocale(user));
    }

    /**
     * Make the custom potion effect like the player usually sees.
     *
     * @param effect The potion effect.
     * @param user   The user's language will be used for translation.
     * @return The translated and formatted potion effect name, level, and duration.
     */
    public String getPotionEffectDisplay(PotionEffect effect, User user) {
        return LanguageHelper.getPotionEffectDisplay(effect, getUserLocale(user));
    }

    /**
     * Translate the type name of tropical fish.
     *
     * @param fishPattern The type of tropical fish.
     * @param user        The user's language will be used for translation.
     * @return The translated name of the tropical fish type.
     */
    public String getTropicalFishTypeName(TropicalFish.Pattern fishPattern, User user) {
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
     *         tropical fish, otherwise return null.
     */
    @Nullable
    public String getPredefinedTropicalFishName(TropicalFishBucketMeta meta, User user) {
        return LanguageHelper.getPredefinedTropicalFishName(meta, getUserLocale(user));
    }

    /**
     * Translate the name of the dye color.
     *
     * @param color The color of the dye.
     * @param user  The user's language will be used for translation.
     * @return The name of the dye color that has been translated.
     */
    public String getDyeColorName(DyeColor color, User user) {
        return LanguageHelper.getDyeColorName(color, getUserLocale(user));
    }

    /**
     * Translate merchant's level name.
     *
     * @param level The merchant's level.
     * @param user  The user's language will be used for translation.
     * @return Translated name of merchant's level.
     */
    public String getVillagerLevelName(int level, User user) {
        return LanguageHelper.getVillagerLevelName(level, getUserLocale(user));
    }

    /**
     * Translate the profession name of the villager.
     *
     * @param profession The villager's profession.
     * @param user       The user's language will be used for translation.
     * @return The translated profession name of the villager.
     */
    public String getVillagerProfessionName(Villager.Profession profession, User user) {
        return LanguageHelper.getVillagerProfessionName(profession, getUserLocale(user));
    }

    /**
     * Translate the name of the banner pattern.
     *
     * @param pattern Contains the color banner pattern.
     * @param user    The user's language will be used for translation.
     * @return The translated name of banner pattern.
     */
    public String getBannerPatternName(Pattern pattern, User user) {
        return LanguageHelper.getBannerPatternName(pattern, getUserLocale(user));
    }

}
