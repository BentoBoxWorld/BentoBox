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
            BentoBox.getInstance().logWarning("ItemStack " + value.getType() + " has quantity " + value.getAmount()
                    + " which is less than 1. Clamping to 1.");
            value = value.clone();
            value.setAmount(1);
        }
        YamlConfiguration c = new YamlConfiguration();
        c.set("is", value);
        out.value(c.saveToString());
    }

    @Override
    public ItemStack read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        YamlConfiguration c = new YamlConfiguration();
        String n = reader.nextString();
        // Verify material type because yaml loading errors of unknown materials cannot be trapped by try clause.
        if (n.contains("type:")) {
            String type = n.substring(n.indexOf("type:") + 6);
            type = type.substring(0, type.indexOf('\n'));
            Material m = Material.matchMaterial(type);            
            if (m == null) {
                BentoBox.getInstance().logWarning("Unknown material: " + type);
                return new ItemStack(Material.AIR);
            }

        }
        try {
            c.loadFromString(n);
            return c.getItemStack("is");
        } catch (InvalidConfigurationException e) {
            BentoBox.getInstance().logError("Cannot load ItemStack serialized as " + n);
            return new ItemStack(Material.AIR);
        }
    }

}
