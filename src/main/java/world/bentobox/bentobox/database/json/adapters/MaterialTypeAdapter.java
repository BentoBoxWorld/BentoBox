package world.bentobox.bentobox.database.json.adapters;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;

import com.google.common.base.Enums;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * Minecraft 1.20 changed GRASS to SHORT_GRASS. This class provides and backwards compatibility when loading
 * database files stored with previous versions. It can be extended in the future if further enum changes are made.
 * @author tastybento
 * @since 2.0.0
 */
public final class MaterialTypeAdapter extends TypeAdapter<Material>
{
    /**
     * Map that contains string value to the actual Material enum object.
     */
    final Map<String, Material> materialMap;

    public MaterialTypeAdapter() {
        this.materialMap = new HashMap<>();

        // Put in current values.
        Arrays.stream(Material.values()).forEach(mat -> this.materialMap.put(mat.name(), mat));

        // Put in renamed material values.
        if (Enums.getIfPresent(Material.class, "SHORT_GRASS").isPresent()) {
            this.materialMap.put("GRASS", Material.SHORT_GRASS);
        }
    }

    @Override
    public Material read(JsonReader input) throws IOException
    {
        if (JsonToken.NULL.equals(input.peek())) {
            input.nextNull();
            return null;
        }

        return this.materialMap.get(input.nextString().toUpperCase());
    }

    @Override
    public void write(JsonWriter output, Material enumValue) throws IOException {
        output.value(enumValue != null ? enumValue.name() : null);
    }
}

