package us.tastybento.bskyblock.util;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
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
        String[] part = s.split(":");
        // Material:Qty
        if (part.length == 2) {
            return two(s, part);
        } else if (part.length == 3) {
            return three(s, part);
        } else if (part.length == 6 && (part[0].contains("POTION") || part[0].equalsIgnoreCase("TIPPED_ARROW"))) {
            return potion(s, part);
        }
        return null;
    }

    private static ItemStack three(String s, String[] part) {
        // Rearrange
        String[] twoer = {part[0], part[2]};
        ItemStack result = two(s, twoer);
        if (result == null) {
            return null;
        }
        if (StringUtils.isNumeric(part[1])) {
            result.setDurability((short) Integer.parseInt(part[1]));
        } else if (result.getType().equals(Material.MONSTER_EGG)) {
            // Check if this is a string
            EntityType entityType = EntityType.valueOf(part[1]);
            SpawnEggMeta meta = ((SpawnEggMeta)result.getItemMeta());
            meta.setSpawnedType(entityType);
            result.setItemMeta(meta);
        }
        return result;
    }

    private static ItemStack two(String s, String[] part) {
        int reqAmount = 0;
        try {
            reqAmount = Integer.parseInt(part[1]);
        } catch (Exception e) {
            return null;
        }

        Material reqItem = Material.getMaterial(part[0].toUpperCase() + "_ITEM");
        if (reqItem == null) {
            // Try the item
            reqItem = Material.getMaterial(part[0].toUpperCase());
        }

        if (reqItem == null) {
            return null;
        }
        return new ItemStack(reqItem, reqAmount);
    }

    private static ItemStack potion(String s, String[] part) {
        int reqAmount = 0;
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
        result.setAmount(reqAmount);
        PotionMeta potionMeta = (PotionMeta)(result.getItemMeta());
        PotionType type = PotionType.valueOf(part[1].toUpperCase());
        boolean isUpgraded = !part[2].isEmpty() && !part[2].equalsIgnoreCase("1");
        boolean isExtended = part[3].equalsIgnoreCase("EXTENDED");
        PotionData data = new PotionData(type, isExtended, isUpgraded);
        potionMeta.setBasePotionData(data);

        result.setAmount(reqAmount);
        return result;
    }
}
