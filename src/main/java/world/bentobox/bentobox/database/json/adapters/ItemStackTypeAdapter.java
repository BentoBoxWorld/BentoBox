package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Serializes ItemStack to JSON and back.
 * I'm going to cheat and use Bukkit's built in YAML serializer/deserializer.
 * This will have the best chance of backwards compatibility with new server versions.
 * @author tastybento
 *
 */
public class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
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
        try {
            c.loadFromString(reader.nextString());
            return c.getItemStack("is");
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

}
