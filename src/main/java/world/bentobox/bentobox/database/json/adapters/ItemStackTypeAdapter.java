package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

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
            // Bukkit uses AIR with amount 0 as its canonical empty-stack sentinel. It is
            // valid application state, not malformed item data, so normalizing it for
            // YAML must not emit an operational warning on every database save.
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

    static boolean isUnexpectedLowAmount(Material type, int amount) {
        return amount < 1 && type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR;
    }

    @Override
    public ItemStack read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        YamlConfiguration c = new YamlConfiguration();
        String n = reader.nextString();

        // 1. Primary deserialization: delegate directly to Bukkit/Paper's native YAML loader
        // so modern 1.20.5+ / 1.21+ Data Components (which can contain nested `type:` keys
        // such as `minecraft:attribute_modifiers`) are preserved without false-positive
        // unknown-material rejections.
        try {
            c.loadFromString(n);
            ItemStack is = c.getItemStack("is");
            if (is != null) {
                return is;
            }
        } catch (InvalidConfigurationException | RuntimeException ignored) {
            // Fall through to legacy material recovery
        }

        // 2. Fallback: attempt best-effort material recovery if native deserialization failed
        if (n.contains("type:")) {
            try {
                String type = n.substring(n.indexOf("type:") + 5);
                int end = type.indexOf('\n');
                if (end >= 0) {
                    type = type.substring(0, end);
                }
                type = type.trim().replace("\"", "").replace("'", "");
                Material m = Material.matchMaterial(type);
                if (m != null) {
                    return new ItemStack(m);
                }
            } catch (RuntimeException ignored) {
                // Fall through to AIR sentinel
            }
        }

        BentoBox.getInstance().logWarning(
                "Cannot load ItemStack safely from data, falling back to AIR: "
                        + (n.length() > 80 ? n.substring(0, 80) + "..." : n));
        return new ItemStack(Material.AIR);
    }

}
