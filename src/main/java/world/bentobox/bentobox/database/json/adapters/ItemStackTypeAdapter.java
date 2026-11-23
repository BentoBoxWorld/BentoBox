package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.BentoBox;

/**
 * Serializes ItemStack to JSON and back.
 * I'm going to cheat and use Bukkit's built in YAML serializer/deserializer.
 * This will have the best chance of backwards compatibility with new server versions.
 * @author tastybento
 *
 */
public class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {

    private static final int MAX_AMOUNT = 99;
    private static final Pattern ITEM_MARKER = Pattern.compile("^(\\s*)==: org\\.bukkit\\.inventory\\.ItemStack\\s*$");
    // Trailing whitespace stays in group 2 and is trimmed by the caller: matching it here with a
    // lazy quantifier made the pattern backtrack.
    private static final Pattern ITEM_KEY = Pattern.compile("(\\s*)(?:id|type):\\s*(\\S.*)");

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        // Clamp quantity to valid serialization range [1, 99]
        if (value.getAmount() > MAX_AMOUNT) {
            BentoBox.getInstance().logWarning("ItemStack " + value.getType() + " has quantity " + value.getAmount()
                    + " which exceeds max " + MAX_AMOUNT + ". Clamping to " + MAX_AMOUNT + ".");
            value = value.clone();
            value.setAmount(MAX_AMOUNT);
        } else if (value.getAmount() < 1) {
            if (isUnexpectedLowAmount(value.getType(), value.getAmount())) {
                BentoBox.getInstance().logWarning("ItemStack " + value.getType() + " has quantity " + value.getAmount()
                        + " which is less than 1. Clamping to 1.");
            }
            value = value.clone();
            value.setAmount(1);
        }
        YamlConfiguration c = new YamlConfiguration();
        c.set("is", value);
        out.value(c.saveToString());
    }

    /**
     * Bukkit uses an AIR stack with amount 0 for an empty slot. That is normal state, not bad
     * item data, so only a real item with an amount below 1 is worth a warning.
     */
    static boolean isUnexpectedLowAmount(Material type, int amount) {
        return amount < 1 && !type.isAir();
    }

    @Override
    public ItemStack read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String n = reader.nextString();
        // Let Bukkit/Paper load the item first. Modern Paper stores items as id/count/components,
        // and component data (attribute modifiers, custom names, ...) can itself contain "type:",
        // so the stored text must not be scanned for a material before this. Bukkit logs and
        // returns null for an item it cannot build, e.g. one with a material this server lacks.
        String cause = "";
        try {
            YamlConfiguration c = new YamlConfiguration();
            c.loadFromString(n);
            ItemStack is = c.getItemStack("is");
            if (is != null) {
                return is;
            }
        } catch (InvalidConfigurationException | RuntimeException e) {
            cause = " (" + e.getMessage() + ")";
        }
        // Fall back to keeping at least the material
        Material m = recoverMaterial(n);
        if (m != null) {
            BentoBox.getInstance().logWarning("Could not fully load ItemStack" + cause + ". Recovered it as a plain " + m
                    + " without its amount or item data. Stored data: " + n);
            return new ItemStack(m);
        }
        BentoBox.getInstance().logError("Cannot load ItemStack" + cause + ". Replacing it with AIR. Stored data: " + n);
        return new ItemStack(Material.AIR);
    }

    /**
     * Finds the material of a stored item from its item-level {@code id:} (modern format) or
     * {@code type:} (legacy format) key. Only keys at the same indent as the item's
     * {@code ==: org.bukkit.inventory.ItemStack} marker count, so a nested {@code type:} inside
     * item data, such as a firework effect, is never mistaken for the material.
     * @param yaml the stored YAML
     * @return the material, or null if none is found or this server does not know it
     */
    static @Nullable Material recoverMaterial(String yaml) {
        String[] lines = yaml.split("\\R");
        String indent = null;
        for (String line : lines) {
            Matcher marker = ITEM_MARKER.matcher(line);
            if (marker.matches()) {
                indent = marker.group(1);
                break;
            }
        }
        if (indent == null) {
            return null;
        }
        for (String line : lines) {
            Matcher key = ITEM_KEY.matcher(line);
            if (key.matches() && key.group(1).equals(indent)) {
                return Material.matchMaterial(key.group(2).replace("\"", "").replace("'", "").trim());
            }
        }
        return null;
    }

}
