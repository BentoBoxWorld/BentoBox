package world.bentobox.bentobox.util;

import java.util.Map.Entry;
import java.util.Optional;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;

/**
 * 
 * Provides localized translations of various objects. Uses LangUtils plugin if available.
 * @author ApacheZy, tastybento
 *
 */
public class LangUtils {

    private static Optional<LangUtilsHook> luHook;

    public static void setHook() {
        luHook = BentoBox.getInstance().getHooks().getHook("LangUtils").map(LangUtilsHook.class::cast);
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
    public static String getItemDisplayName(ItemStack item, User user) {
        return luHook.map(h -> h.getItemDisplayName(item, user)).orElseGet(() -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String dname = meta.getDisplayName();
                if (!dname.isEmpty()) {
                    return dname;
                }
            }
            return Util.prettifyText(item.getType().name());
        });
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
    public static String getItemName(ItemStack itemStack, User user) {
        return luHook.map(h -> h.getItemName(itemStack, user)).orElseGet(() -> Util.prettifyText(itemStack.getType().name()));
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
    public static String getMaterialName(Material material, User user) {
        return luHook.map(h -> h.getMaterialName(material, user)).orElseGet(() -> Util.prettifyText(material.name()));
    }

    /**
     * Return the display name of the entity.
     *
     * @param entity The entity
     * @param user   the User's locale will be used for translation.
     * @return The name of the entity
     */
    public static String getEntityDisplayName(Entity entity, User user) {
        return entity.getCustomName() != null
                ? entity.getCustomName()
                        : getEntityName(entity, user);
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entityType the EntityType whose name will be translated.
     * @param user       the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    public static String getEntityName(EntityType entityType, User user) {
        return luHook.map(h -> h.getEntityName(entityType, user)).orElseGet(() -> Util.prettifyText(entityType.toString()));
    }

    /**
     * Translate the name of the entity type.
     *
     * @param entity the EntityType whose name will be translated.
     * @param user   the User's locale will be used for translation.
     * @return The translated EntityType name.
     */
    public static String getEntityName(Entity entity, User user) {
        return getEntityName(entity.getType(), user);
    }

    /**
     * Translate the name of the Biome.
     *
     * @param biome the Biome whose name will be translated.
     * @param user  the User's locale will be used for translation.
     * @return The translated Biome name.
     */
    public static String getBiomeName(Biome biome, User user) {
        return luHook.map(h -> h.getBiomeName(biome, user)).orElseGet(() -> Util.prettifyText(biome.name()));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param ench  The enchantment.
     * @param level The enchantment level.
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    public static String getEnchantDisplayName(Enchantment ench, int level, User user) {
        return luHook.map(h -> h.getEnchantDisplayName(ench, level, user)).orElseGet(() -> Util.prettifyText(ench.getKey().getKey() + " " + level));
    }

    /**
     * Return the display name of the enchantment(with level).
     *
     * @param entry The Entry of an enchantment with level. The type
     *              is {@code Map.Entry<Enchantment, Integer>}
     * @param user  The User's locale will be used for translation.
     * @return Translated enchanted name with level.
     */
    public static String getEnchantDisplayName(Entry<Enchantment, Integer> entry, User user) {
        return getEnchantDisplayName(entry.getKey(), entry.getValue(), user);
    }

    /**
     * Return the name of the enchantment.
     *
     * @param enchant The enchantment.
     * @param user    The User's locale will be used for translation.
     * @return The translated enchant name.
     */
    public static String getEnchantName(Enchantment enchant, User user) {
        return luHook.map(h -> h.getEnchantName(enchant, user)).orElseGet(() -> Util.prettifyText(enchant.getKey().getKey()));
    }

    /**
     * Return the enchantment level indicated by Roman numerals.
     * Can only get Roman numerals within 10.
     *
     * @param level The enchantment level.
     * @param user  The user's language will be used for translation.
     * @return The converted enchantment level.
     */
    public static String getEnchantLevelName(int level, User user) {
        return luHook.map(h -> h.getEnchantLevelName(level, user)).orElseGet(() -> String.valueOf(level));
    }

    /**
     * Translate the name of the potion.
     *
     * @param potionType The type of the potion.
     * @param user       The user's language will be used for translation.
     * @return Translated potion name.
     */
    public static String getPotionTypeName(PotionType potionType, User user) {
        return luHook.map(h -> h.getPotionTypeName(potionType, user)).orElseGet(() -> {
            switch (potionType) {
            case UNCRAFTABLE:     return "Uncraftable Potion";
            case WATER:           return "Water Bottle";
            case MUNDANE:         return "Mundane Potion";
            case THICK:           return "Thick Potion";
            case AWKWARD:         return "Awkward Potion";
            case NIGHT_VISION:    return "Potion of Night Vision";
            case INVISIBILITY:    return "Potion of Invisibility";
            case JUMP:            return "Potion of Leaping";
            case FIRE_RESISTANCE: return "Potion of Fire Resistance";
            case SPEED:           return "Potion of Swiftness";
            case SLOWNESS:        return "Potion of Slowness";
            case WATER_BREATHING: return "Potion of Water Breathing";
            case INSTANT_HEAL:    return "Potion of Healing";
            case INSTANT_DAMAGE:  return "Potion of Harming";
            case POISON:          return "Potion of Poison";
            case REGEN:           return "Potion of Regeneration";
            case STRENGTH:        return "Potion of Strength";
            case WEAKNESS:        return "Potion of Weakness";
            case LUCK:            return "Potion of Luck";
            case TURTLE_MASTER:   return "Potion of the Turtle Master";
            case SLOW_FALLING:    return "Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
            }
        });

    }

    /**
     * Translate the name of the splash potion.
     *
     * @param potionType The type of the splash potion.
     * @param user       The user's language will be used for translation.
     * @return Translated splash potion name.
     */
    public static String getSplashPotionName(PotionType potionType, User user) {
        return luHook.map(h -> h.getSplashPotionName(potionType, user)).orElseGet(() -> {     
            switch (potionType) {
            case UNCRAFTABLE:     return "Splash Uncraftable Potion";
            case WATER:           return "Splash Water Bottle";
            case MUNDANE:         return "Mundane Splash Potion";
            case THICK:           return "Thick Splash Potion";
            case AWKWARD:         return "Awkward Splash Potion";
            case NIGHT_VISION:    return "Splash Potion of Night Vision";
            case INVISIBILITY:    return "Splash Potion of Invisibility";
            case JUMP:            return "Splash Potion of Leaping";
            case FIRE_RESISTANCE: return "Splash Potion of Fire Resistance";
            case SPEED:           return "Splash Potion of Swiftness";
            case SLOWNESS:        return "Splash Potion of Slowness";
            case WATER_BREATHING: return "Splash Potion of Water Breathing";
            case INSTANT_HEAL:    return "Splash Potion of Healing";
            case INSTANT_DAMAGE:  return "Splash Potion of Harming";
            case POISON:          return "Splash Potion of Poison";
            case REGEN:           return "Splash Potion of Regeneration";
            case STRENGTH:        return "Splash Potion of Strength";
            case WEAKNESS:        return "Splash Potion of Weakness";
            case LUCK:            return "Splash Potion of Luck";
            case TURTLE_MASTER:   return "Splash Potion of the Turtle Master";
            case SLOW_FALLING:    return "Splash Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
            }
        });
    }

    /**
     * Translate the name of the lingering potion.
     *
     * @param potionType The type of lingering potion.
     * @param user       The user's language will be used for translation.
     * @return Translated lingering potion name.
     */
    public static String getLingeringPotionName(PotionType potionType, User user) {
        return luHook.map(h -> h.getLingeringPotionName(potionType, user)).orElseGet(() -> {
            switch (potionType) {
            case UNCRAFTABLE:     return "Lingering Uncraftable Potion";
            case WATER:           return "Lingering Water Bottle";
            case MUNDANE:         return "Mundane Lingering Potion";
            case THICK:           return "Thick Lingering Potion";
            case AWKWARD:         return "Awkward Lingering Potion";
            case NIGHT_VISION:    return "Lingering Potion of Night Vision";
            case INVISIBILITY:    return "Lingering Potion of Invisibility";
            case JUMP:            return "Lingering Potion of Leaping";
            case FIRE_RESISTANCE: return "Lingering Potion of Fire Resistance";
            case SPEED:           return "Lingering Potion of Swiftness";
            case SLOWNESS:        return "Lingering Potion of Slowness";
            case WATER_BREATHING: return "Lingering Potion of Water Breathing";
            case INSTANT_HEAL:    return "Lingering Potion of Healing";
            case INSTANT_DAMAGE:  return "Lingering Potion of Harming";
            case POISON:          return "Lingering Potion of Poison";
            case REGEN:           return "Lingering Potion of Regeneration";
            case STRENGTH:        return "Lingering Potion of Strength";
            case WEAKNESS:        return "Lingering Potion of Weakness";
            case LUCK:            return "Lingering Potion of Luck";
            case TURTLE_MASTER:   return "Lingering Potion of the Turtle Master";
            case SLOW_FALLING:    return "Lingering Potion of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
            }
        });
    }

    /**
     * Translate the name of the tipped arrow.
     *
     * @param potionType Potion type of tipped arrow.
     * @param user       The user's language will be used for translation.
     * @return Translated tipped arrow name.
     */
    public static String getTippedArrowName(PotionType potionType, User user) {
        return luHook.map(h -> h.getTippedArrowName(potionType, user)).orElseGet(() -> {
            switch (potionType) {
            case UNCRAFTABLE:     return "Uncraftable Tipped Arrow";
            case WATER:           return "Arrow of Splashing";
            case MUNDANE:
            case THICK:
            case AWKWARD:         return "Tipped Arrow";
            case NIGHT_VISION:    return "Arrow of Night Vision";
            case INVISIBILITY:    return "Arrow of Invisibility";
            case JUMP:            return "Arrow of Leaping";
            case FIRE_RESISTANCE: return "Arrow of Fire Resistance";
            case SPEED:           return "Arrow of Swiftness";
            case SLOWNESS:        return "Arrow of Slowness";
            case WATER_BREATHING: return "Arrow of Water Breathing";
            case INSTANT_HEAL:    return "Arrow of Healing";
            case INSTANT_DAMAGE:  return "Arrow of Harming";
            case POISON:          return "Arrow of Poison";
            case REGEN:           return "Arrow of Regeneration";
            case STRENGTH:        return "Arrow of Strength";
            case WEAKNESS:        return "Arrow of Weakness";
            case LUCK:            return "Arrow of Luck";
            case TURTLE_MASTER:   return "Arrow of the Turtle Master";
            case SLOW_FALLING:    return "Arrow of Slow Falling";
            default:
                return Util.prettifyText(potionType.name());
            }
        });
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
    public static String getPotionBaseEffectName(PotionType potionType, User user) {
        return luHook.map(h -> h.getPotionBaseEffectName(potionType, user)).orElseGet(() -> {
            PotionEffectType effectType = potionType.getEffectType();
            if (effectType == null) {
                return "No Effects";
            }
            return Util.prettifyText(effectType.getName());
        });
    }

    /**
     * Translate the name of the potion effect.
     *
     * @param effectType The potion effect.
     * @param user       The user's language will be used for translation.
     * @return Translated name of potion effect.
     */
    public static String getPotionEffectName(PotionEffectType effectType, User user) {
        return luHook.map(h -> h.getPotionEffectName(effectType, user)).orElseGet(() -> Util.prettifyText(effectType.getName()));
    }

    /**
     * Translate the name of the potion level.
     *
     * @param amplifier The  potion level.
     * @param user      The user's language will be used for translation.
     * @return The translated name of the potion level.
     */
    public static String getEffectAmplifierName(int amplifier, User user) {
        return luHook.map(h -> h.getEffectAmplifierName(amplifier, user)).orElseGet(() -> amplifier > 0 ? Integer.toString(amplifier) : "");
    }

    /**
     * Make the custom potion effect like the player usually sees.
     *
     * @param effect The potion effect.
     * @param user   The user's language will be used for translation.
     * @return The translated and formatted potion effect name, level, and duration.
     */
    public static String getPotionEffectDisplay(PotionEffect effect, User user) {
        return luHook.map(h -> h.getPotionEffectDisplay(effect, user)).orElseGet(() -> {
            String effecName = getPotionEffectName(effect.getType(), user);
            String amplifier = getEffectAmplifierName(effect.getAmplifier(), user);

            if (amplifier.length() > 0) {
                effecName = effecName + " " + amplifier;
            }

            int duration = effect.getDuration();
            if (duration > 20) {
                int m = duration / 20 / 60;
                int s = duration / 20 % 60;
                String ts = String.format("%d:%02d", m, s);
                effecName = String.format("%s (%s)", effecName, ts);
            }

            return effecName;
        });
    }

    /**
     * Translate the type name of tropical fish.
     *
     * @param fishPattern The type of tropical fish.
     * @param user        The user's language will be used for translation.
     * @return The translated name of the tropical fish type.
     */
    public static String getTropicalFishTypeName(TropicalFish.Pattern fishPattern, User user) {
        return luHook.map(h -> h.getTropicalFishTypeName(fishPattern, user)).orElseGet(() -> Util.prettifyText(fishPattern.name()));
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
    public static String getPredefinedTropicalFishName(TropicalFishBucketMeta meta, User user) {
        return luHook.map(h -> h.getPredefinedTropicalFishName(meta, user)).orElseGet(() -> {

            if (meta.hasVariant()) {
                TropicalFish.Pattern pattern = meta.getPattern();

                // https://minecraft.gamepedia.com/Tropical_Fish#Entity_data

                int type = pattern.ordinal() > 5 ? 1 : 0;
                int patt = pattern.ordinal() % 6;
                int bcol = meta.getBodyColor().ordinal();
                int pcol = meta.getPatternColor().ordinal();

                int variant = (pcol & 255) << 24 | (bcol & 255) << 16 | (patt & 255) << 8 | type;

                switch (variant) {
                case 117506305: return "Anemone";
                case 117899265: return "Black Tang";
                case 185008129: return "Blue Tang";
                case 117441793: return "Butterflyfish";
                case 118161664: return "Cichlid";
                case 65536    : return "Clownfish";
                case 50726144 : return "Cotton Candy Betta";
                case 67764993 : return "Dottyback";
                case 234882305: return "Emperor Red Snapper";
                case 67110144 : return "Goatfish";
                case 117441025: return "Moorish Idol";
                case 16778497 : return "Ornate Butterflyfish";
                case 101253888: return "Parrotfish";
                case 50660352 : return "Queen Angelfish";
                case 918529   : return "Red Cichlid";
                case 235340288: return "Red Lipped Blenny";
                case 918273   : return "Red Snapper";
                case 67108865 : return "Threadfin";
                case 917504   : return "Tomato Clownfish";
                case 459008   : return "Triggerfish";
                case 67699456 : return "Yellowtail Parrotfish";
                case 67371009 : return "Yellow Tang";
                default       : break;
                }
            }
            return null;
        });
    }

    /**
     * Translate the name of the dye color.
     *
     * @param color The color of the dye.
     * @param user  The user's language will be used for translation.
     * @return The name of the dye color that has been translated.
     */
    public static String getDyeColorName(DyeColor color, User user) {
        return luHook.map(h -> h.getDyeColorName(color, user)).orElseGet(() -> Util.prettifyText(color.name()));
    }

    /**
     * Translate merchant's level name.
     *
     * @param level The merchant's level.
     * @param user  The user's language will be used for translation.
     * @return Translated name of merchant's level.
     */
    public static String getVillagerLevelName(int level, User user) {
        return luHook.map(h -> h.getVillagerLevelName(level, user)).orElseGet(() -> String.valueOf(level));
    }

    /**
     * Translate the profession name of the villager.
     *
     * @param profession The villager's profession.
     * @param user       The user's language will be used for translation.
     * @return The translated profession name of the villager.
     */
    public static String getVillagerProfessionName(Villager.Profession profession, User user) {
        return luHook.map(h -> h.getVillagerProfessionName(profession, user)).orElseGet(() -> Util.prettifyText(profession.name()));
    }

    /**
     * Translate the name of the banner pattern.
     *
     * @param pattern Contains the color banner pattern.
     * @param user    The user's language will be used for translation.
     * @return The translated name of banner pattern.
     */
    public static String getBannerPatternName(Pattern pattern, User user) {
        return luHook.map(h -> h.getBannerPatternName(pattern, user)).orElseGet(() -> Util.prettifyText(pattern.getColor().name()
                        + " "
                        + pattern.getPattern().name()));
    }

    /**
     * Get the description of the music disk.
     *
     * @param material Material for music records.
     * @param user     This parameter is temporarily useless and is reserved for
     *                 possible future changes.
     * @return If the given material is a music disk, the description of the
     *         music disk is returned. Otherwise, return NULL.
     */
    @Nullable
    public static String getMusicDiskDesc(Material material, User user) {

        // The description of the music record is the same in any language,
        // so directly output it here.

        switch (material) {
        case MUSIC_DISC_13      : return "C418 - 13";
        case MUSIC_DISC_CAT     : return "C418 - cat";
        case MUSIC_DISC_BLOCKS  : return "C418 - blocks";
        case MUSIC_DISC_CHIRP   : return "C418 - chirp";
        case MUSIC_DISC_FAR     : return "C418 - far";
        case MUSIC_DISC_MALL    : return "C418 - mall";
        case MUSIC_DISC_MELLOHI : return "C418 - mellohi";
        case MUSIC_DISC_STAL    : return "C418 - stal";
        case MUSIC_DISC_STRAD   : return "C418 - strad";
        case MUSIC_DISC_WARD    : return "C418 - ward";
        case MUSIC_DISC_11      : return "C418 - 11";
        case MUSIC_DISC_WAIT    : return "C418 - wait";
        case MUSIC_DISC_PIGSTEP : return "Lena Raine - Pigstep";
        default                 : return null;
        }
    }

}
