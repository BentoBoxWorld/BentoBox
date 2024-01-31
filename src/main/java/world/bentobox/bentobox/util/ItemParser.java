package world.bentobox.bentobox.util;

import java.net.URL;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import world.bentobox.bentobox.BentoBox;


/**
 * Utility class that parses a String into an ItemStack.
 * It is used for converting config file entries to objects.
 *
 * @author tastybento, Poslovitch
 */
@SuppressWarnings("deprecation")
public class ItemParser {

    private ItemParser() {} // private constructor to hide the implicit public one.
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

        ItemStack returnValue;

        try {
            // Check if item can be parsed using bukkit item factory.
            returnValue = Bukkit.getItemFactory().createItemStack(text);
        }
        catch (IllegalArgumentException exception) {
            returnValue = ItemParser.parseOld(text, defaultItemStack);
        }

        return returnValue;
    }


    /**
     * Parse given string to ItemStack.
     * @param text String value of item stack.
     * @param defaultItemStack Material that should be returned if parsing failed.
     * @return ItemStack of parsed item or defaultItemStack.
     */
    @Nullable
    private static ItemStack parseOld(@Nullable String text, @Nullable ItemStack defaultItemStack) {

        if (text == null || text.isBlank()) {
            // Text does not exist or is empty.
            return defaultItemStack;
        }

        ItemStack returnValue = defaultItemStack;

        String[] part = text.split(":");

        try {
            // Because I am lazy, and do not want to rewrite every parser, I will just add custom data as
            // parameter and remove that array part form input data.
            Optional<String> first = Arrays.stream(part).filter(field -> field.matches("(CMD-\\d*)")).findFirst();
            Integer customModelData = null;
            if (first.isPresent()) {
                // Ugly and fast way how to get rid of customData field.
                String[] copyParts = new String[part.length - 1];
                int j = 0;

                for (String field : part) {
                    if (!field.matches("(CMD-\\d*)")) {
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
                returnValue = new ItemStack(Material.valueOf(part[0].toUpperCase()));
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

            // Update item meta with custom data model.
            if (returnValue != null && customModelData != null) {
                ItemParser.setCustomModelData(returnValue, customModelData);
            }
        } catch (Exception exception) {
            BentoBox.getInstance().logError("Could not parse item " + text + " " + exception.getLocalizedMessage());
            returnValue = defaultItemStack;
        }

        return returnValue;
    }


    /**
     * This method assigns custom model data to the item stack.
     * @param returnValue Item stack that should be updated.
     * @param customModelData Integer value of custom model data.
     */
    private static void setCustomModelData(ItemStack returnValue, Integer customModelData) {
        // We have custom data model. Now assign it to the item-stack.
        ItemMeta itemMeta = returnValue.getItemMeta();

        // Another null-pointer check for materials that does not have item meta.
        if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
            // Update meta to the return item.
            returnValue.setItemMeta(itemMeta);
        }
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

        if (meta instanceof Damageable damageable) {
            damageable.setDamage(Integer.parseInt(part[1]));
            durability.setItemMeta(meta);
        }

        return durability;
    }


    /**
     * This method parses array of 6 items into an item stack.
     * Format:
     * <pre>{@code
     *      POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY
     * }</pre>
     * Example:
     * <pre>{@code
     *      POTION:STRENGTH:1:EXTENDED:SPLASH:1
     * }</pre>
     * @param part String array that contains 6 elements.
     * @return Potion with given properties.
     * @deprecated due to the spigot potion changes.
     */
    @Deprecated
    private static ItemStack parsePotionOld(String[] part) {
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
     * This method parses array of 6 items into an item stack.
     * Format:
     * <pre>{@code
     *      POTION:<POTION_TYPE>:QTY
     * }</pre>
     * Example:
     * <pre>{@code
     *      POTION:STRENGTH:1
     * }</pre>
     * @link <a href="https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionType.html">Potion Type</a>
     * @param part String array that contains 3 elements.
     * @return Potion with given properties.
     */
    private static ItemStack parsePotion(String[] part) {
        if (part.length == 6) {
            BentoBox.getInstance().logWarning("The old potion parsing detected for " + part[0] +
                ". Please update your configs, as SPIGOT changed potion types.");
            return parsePotionOld(part);
        }

        if (part.length != 3) {
            throw new MissingFormatArgumentException("Potion parsing requires 3 parts.");
        }

        /*
            # Format POTION:<POTION_TYPE>:QTY
            # Potion Type can be found out in: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionType.html
            # Examples:
            # POTION:STRENGTH:1
            # POTION:INSTANT_DAMAGE:2
            # POTION:JUMP:1
            # POTION:WEAKNESS:1   -  any weakness potion
         */

        Material material = Material.matchMaterial(part[0]);

        if (material == null) {
            BentoBox.getInstance().logWarning("Could not parse potion item " + part[0] + " so using a regular potion.");
            material = Material.POTION;
        }

        ItemStack result = new ItemStack(material, Integer.parseInt(part[2]));

        if (result.getItemMeta() instanceof PotionMeta meta) {
            PotionType potionType = Enums.getIfPresent(PotionType.class, part[1].toUpperCase(Locale.ENGLISH)).
                or(PotionType.WATER);
            meta.setBasePotionType(potionType);
            result.setItemMeta(meta);
        }

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
     * <pre>{@code
     *    PLAYER_HEAD:<STRING/Trimmed UUID/UUID/Texture>:QTY
     *    PLAYER_HEAD:<STRING/Trimmed UUID/UUID/Texture>
     *    PLAYER_HEAD:QTY
     * }</pre>
     * Example:
     * <pre>{@code
     *    PLAYER_HEAD:1
     *    PLAYER_HEAD:BONNe1704
     *    PLAYER_HEAD:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWY1ZjE1OTg4NmNjNTMxZmZlYTBkOGFhNWY5MmVkNGU1ZGE2NWY3MjRjMDU3MGFmODZhOTBiZjAwYzY3YzQyZSJ9fX0:1
     * }</pre>
     * @param part String array that contains at least 2 elements.
     * @return Player head with given properties.
     */
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
            if (playerHead.getItemMeta() instanceof SkullMeta meta)
            {
                PlayerProfile profile;

                if (part[1].length() < 17) {
                    // Minecraft player names are in length between 3 and 16 chars.
                    profile = Bukkit.createPlayerProfile(part[1]);
                } else if (part[1].length() == 32) {
                    // trimmed UUID length are 32 chars.
                    profile = Bukkit.createPlayerProfile(UUID.fromString(part[1].replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
                } else if (part[1].length() == 36) {
                    // full UUID length are 36 chars.
                    profile = Bukkit.createPlayerProfile(UUID.fromString(part[1]));
                } else {
                    // If chars are more than 36, apparently it is base64 encoded texture.
                    profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                    profile.getTextures().setSkin(ItemParser.getSkinURLFromBase64(part[1]));
                }

                // Apply item meta.
                meta.setOwnerProfile(profile);
                playerHead.setItemMeta(meta);
            }
        } catch (Exception ignored) {
            // Could not parse player head.
            BentoBox.getInstance().logError("Could not parse player head item " + part[1] + " so using a Steve head.");
        }

        return playerHead;
    }


    /**
     * This method parses base64 encoded string into URL.
     * @param base64 Base64 encoded string.
     * @return URL of the skin.
     */
    private static URL getSkinURLFromBase64(String base64) {
        /*
         * Base64 encoded string is in format: { "timestamp": 0, "profileId": "UUID",
         * "profileName": "USERNAME", "textures": { "SKIN": { "url":
         * "https://textures.minecraft.net/texture/TEXTURE_ID" }, "CAPE": { "url":
         * "https://textures.minecraft.net/texture/TEXTURE_ID" } } }
         */
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            JsonObject json = new Gson().fromJson(decoded, JsonObject.class);
            String url = json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            return new URL(url);
        }
        catch (Exception e) {
            return null;
        }
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
