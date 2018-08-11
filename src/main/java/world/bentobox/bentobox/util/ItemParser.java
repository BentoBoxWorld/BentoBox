package world.bentobox.bentobox.util;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Utility class that parses a String into an ItemStack.
 * It is used for converting config file entries to objects.
 *
 * @author tastybento, Poslovitch
 */
public class ItemParser {

    private ItemParser() {}

    public static ItemStack parse(String s){
        if (s == null) {
            return null;
        }
        String[] part = s.split(":");

        // Material-specific handling
        if (part[0].contains("POTION") || part[0].equalsIgnoreCase("TIPPED_ARROW")) {
            return potion(part);
        } else if (part[0].contains("BANNER")) {
            return banner(part);
        }

        // Generic handling
        if (part.length == 2) {
            // Material:Qty
            return two(part);
        } else if (part.length == 3) {
            // Material:Durability:Qty
            return three(part);
        }
        return null;
    }

    private static ItemStack two(String[] part) {
        int reqAmount;
        try {
            reqAmount = Integer.parseInt(part[1]);
        } catch (Exception e) {
            return null;
        }

        Material reqItem = Material.getMaterial(part[0].toUpperCase(java.util.Locale.ENGLISH));
        if (reqItem == null) {
            return null;
        }
        return new ItemStack(reqItem, reqAmount);
    }

    private static ItemStack three(String[] part) {
        // Rearrange
        String[] twoer = {part[0], part[2]};
        ItemStack result = two(twoer);
        if (result == null) {
            return null;
        }
        return result;
    }

    private static ItemStack potion(String[] part) {
        if (part.length != 6) {
            return null;
        }
        int reqAmount;
        try {
            reqAmount = Integer.parseInt(part[5]);
        } catch (Exception e) {
            return null;
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

        result.setAmount(reqAmount);
        return result;
    }

    private static ItemStack banner(String[] part) {
        try {
            if (part.length >= 2) {
                ItemStack result = new ItemStack(Material.getMaterial(part[0]), Integer.parseInt(part[1]));

                BannerMeta meta = (BannerMeta) result.getItemMeta();
                for (int i = 2; i < part.length; i += 2) {
                    meta.addPattern(new Pattern(DyeColor.valueOf(part[i + 1]), PatternType.valueOf(part[i])));
                }

                result.setItemMeta(meta);

                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
