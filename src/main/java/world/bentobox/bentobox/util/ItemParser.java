package world.bentobox.bentobox.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.MissingFormatArgumentException;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.eclipse.jdt.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import world.bentobox.bentobox.BentoBox;


/**
 * Utility class that parses a String into an ItemStack.
 * It is used for converting config file entries to objects.
 *
 * @author tastybento, Poslovitch
 */
public class ItemParser {

    /**
     * Parse given string to ItemStack.
     * @param text String value of item stack.
     * @return ItemStack of parsed item or null.
     */
    public static ItemStack parse(String text) {
        return ItemParser.parse(text, null);
    }


    /**
     * Parse given string to ItemStack.
     * @param text String value of item stack.
     * @param defaultItemStack Material that should be returned if parsing failed.
     * @return ItemStack of parsed item or defaultItemStack.
     */
    @Nullable
    public static ItemStack parse(@Nullable String text, @Nullable ItemStack defaultItemStack) {
        if (text == null || text.isBlank()) {
            // Text does not exist or is empty.
            return defaultItemStack;
        }

        ItemStack returnValue = defaultItemStack;

        String[] part = text.split(":");

        try {
            // Because I am lazy, and do not want to rewrite every parser, I will just add custom data as
            // parameter and remove that array part form input data.
            Optional<String> first = Arrays.stream(part).filter(field -> field.matches("(CMD-[0-9]*)")).findFirst();
            Integer customModelData = null;

            if (first.isPresent()) {
                // Ugly and fast way how to get rid of customData field.
                String[] copyParts = new String[part.length - 1];
                int j = 0;

                for (String field : part) {
                    if (!field.matches("(CMD-[0-9]*)")) {
                        copyParts[j++] = field;
                    }
                }

                // Replace original parts with the copy parts that does not have any CMD values.
                part = copyParts;
                // Now use value from Custom Data Model and parse it as integer.
                customModelData = Integer.valueOf(first.get().replaceFirst("CMD-", ""));
            }

            // Check if there are more properties for the item stack
            if (part.length == 1) {
                // Parse material directly. It does not have any extra properties.
                returnValue = new ItemStack(Material.valueOf(text.toUpperCase()));
            }
            // Material-specific handling
            else if (part[0].contains("POTION") || part[0].equalsIgnoreCase("TIPPED_ARROW")) {
                // Parse Potions and Tipped Arrows
                returnValue = parsePotion(part);
            } else if (part[0].contains("BANNER")) {
                // Parse Banners
                returnValue = parseBanner(part);
            } else if (part[0].equalsIgnoreCase("PLAYER_HEAD")) {
                // Parse Player Heads
                returnValue = parsePlayerHead(part);
            }
            // Generic handling
            else if (part.length == 2) {
                // Material:Qty
                returnValue = parseItemQuantity(part);
            } else if (part.length == 3) {
                // Material:Durability:Qty
                returnValue = parseItemDurabilityAndQuantity(part);
            }

            if (returnValue != null) {
                // If wrapper is just for code-style null-pointer checks.
                if (customModelData != null) {
                    // We have custom data model. Now assign it to the item-stack.
                    ItemMeta itemMeta = returnValue.getItemMeta();

                    // Another null-pointer check for materials that does not have item meta.
                    if (itemMeta != null) {
                        itemMeta.setCustomModelData(customModelData);
                        // Update meta to the return item.
                        returnValue.setItemMeta(itemMeta);
                    }
                }
            }
        } catch (Exception exception) {
            BentoBox.getInstance().logError("Could not parse item " + text + " " + exception.getLocalizedMessage());
            returnValue = defaultItemStack;
        }

        return returnValue;
    }


    /**
     * This method parses array of 2 items into an item stack.
     * First array element is material, while second array element is integer, that represents item count.
     * Example:
     *      DIAMOND:20
     * @param part String array that contains 2 elements.
     * @return ItemStack with material from first array element and amount based on second array element.
     */
    private static ItemStack parseItemQuantity(String[] part) {
        int reqAmount = Integer.parseInt(part[1]);
        Material reqItem = Material.getMaterial(part[0].toUpperCase(java.util.Locale.ENGLISH));

        if (reqItem == null) {
            throw new IllegalArgumentException(part[0] + " is not a valid Material.");
        }

        return new ItemStack(reqItem, reqAmount);
    }


    /**
     * This method parses array of 3 items into an item stack.
     * First array element is material, while second and third array element are integers.
     * The middle element represents durability, while third element represents quantity.
     * Example:
     *      IRON_SWORD:20:1
     * @param part String array that contains 3 elements.
     * @return ItemStack with material from first array element, durability from second element and amount based on third array element.
     */
    private static ItemStack parseItemDurabilityAndQuantity(String[] part) {
        // Rearrange
        String[] parsable = {part[0], part[2]};
        ItemStack durability = parseItemQuantity(parsable);

        ItemMeta meta = durability.getItemMeta();

        if (meta instanceof Damageable) {
            ((Damageable) meta).setDamage(Integer.parseInt(part[1]));
            durability.setItemMeta(meta);
        }

        return durability;
    }


    /**
     * This method parses array of 6 items into an item stack.
     * Format:
     *      POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY
     * Example:
     *      POTION:STRENGTH:1:EXTENDED:SPLASH:1
     * @param part String array that contains 6 elements.
     * @return Potion with given properties.
     */
    private static ItemStack parsePotion(String[] part) {
        if (part.length != 6) {
            throw new MissingFormatArgumentException("Potion parsing requires 6 parts.");
        }

        /*
         * # Format POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY
            # LEVEL, EXTENDED, SPLASH, LINGER are optional.
            # LEVEL is a number, 1 or 2
            # LINGER is for V1.9 servers and later
            # Examples:
            # POTION:STRENGTH:1:EXTENDED:SPLASH:1
            # POTION:INSTANT_DAMAGE:2::LINGER:2
            # POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1
            # POTION:WEAKNESS::::1   -  any weakness potion
         */
        ItemStack result = new ItemStack(Material.POTION);
        if (part[4].equalsIgnoreCase("SPLASH")) {
            result = new ItemStack(Material.SPLASH_POTION);
        } else if (part[4].equalsIgnoreCase("LINGER")) {
            result = new ItemStack(Material.LINGERING_POTION);
        }
        if (part[0].equalsIgnoreCase("TIPPED_ARROW")) {
            result = new ItemStack(Material.TIPPED_ARROW);
        }
        PotionMeta potionMeta = (PotionMeta)(result.getItemMeta());
        PotionType type = PotionType.valueOf(part[1].toUpperCase(java.util.Locale.ENGLISH));
        boolean isUpgraded = !part[2].isEmpty() && !part[2].equalsIgnoreCase("1");
        boolean isExtended = part[3].equalsIgnoreCase("EXTENDED");
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        potionMeta.setBasePotionData(data);
        result.setItemMeta(potionMeta);
        result.setAmount(Integer.parseInt(part[5]));
        return result;
    }


    /**
     * This method parses array of multiple elements for the Banner.
     * @param part String array that contains at least 2 elements.
     * @return Banner as item stack.
     */
    private static ItemStack parseBanner(String[] part) {
        if (part.length >= 2) {
            Material bannerMat = Material.getMaterial(part[0]);
            if (bannerMat == null) {
                BentoBox.getInstance().logError("Could not parse banner item " + part[0] + " so using a white banner.");
                bannerMat = Material.WHITE_BANNER;
            }
            ItemStack result = new ItemStack(bannerMat, Integer.parseInt(part[1]));

            BannerMeta meta = (BannerMeta) result.getItemMeta();
            if (meta != null) {
                for (int i = 2; i < part.length; i += 2) {
                    meta.addPattern(new Pattern(DyeColor.valueOf(part[i + 1]), PatternType.valueOf(part[i])));
                }
                result.setItemMeta(meta);
            }

            return result;
        } else {
            throw new MissingFormatArgumentException("Banner parsing requires at least 2 parts.");
        }
    }


    /**
     * This method parses array of 2 to 3 elements that represents player head.
     * Format:
     *    PLAYER_HEAD:<STRING/Trimmed UUID/UUID/Texture>:QTY
     *    PLAYER_HEAD:<STRING/Trimmed UUID/UUID/Texture>
     *    PLAYER_HEAD:QTY
     * Example:
     *    PLAYER_HEAD:1
     *    PLAYER_HEAD:BONNe1704
     *    PLAYER_HEAD:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWY1ZjE1OTg4NmNjNTMxZmZlYTBkOGFhNWY5MmVkNGU1ZGE2NWY3MjRjMDU3MGFmODZhOTBiZjAwYzY3YzQyZSJ9fX0:1
     * @param part String array that contains at least 2 elements.
     * @return Player head with given properties.
     */
    @SuppressWarnings("deprecation")
    private static ItemStack parsePlayerHead(String[] part) {
        ItemStack playerHead;

        if (part.length == 3) {
            String[] parsable = {part[0], part[2]};
            // create parse item and quantity.
            playerHead = parseItemQuantity(parsable);
        } else if (isNumeric(part[1])) {
            // there is no meta item for player head.
            return parseItemQuantity(part);
        } else {
            // create new player head item stack.
            playerHead = new ItemStack(Material.PLAYER_HEAD);
        }

        // Set correct Skull texture
        try {
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

            if (part[1].length() < 17) {
                // Minecraft player names are in length between 3 and 16 chars.
                meta.setOwner(part[1]);
            } else if (part[1].length() == 32) {
                // trimmed UUID length are 32 chars.
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(
                        UUID.fromString(part[1].replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"))));
            } else if (part[1].length() == 36) {
                // full UUID length are 36 chars.
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(part[1])));
            } else {
                // If chars are more than 36, apparently it is base64 encoded texture.
                GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                profile.getProperties().put("textures", new Property("textures", part[1]));

                // Null pointer will be caught and ignored.
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }

            // Apply new meta to the item.
            playerHead.setItemMeta(meta);
        } catch (Exception ignored) {}

        return playerHead;
    }


    /**
     * Check if given sting is an integer.
     * @param string Value that must be checked.
     * @return {@code true} if value is integer, {@code false} otherwise.
     */
    private static boolean isNumeric(String string) {
        if(string == null || string.equals("")) {
            return false;
        }

        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
