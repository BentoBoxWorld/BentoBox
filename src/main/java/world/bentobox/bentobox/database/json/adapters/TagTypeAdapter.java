package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * @author tastybento
 *
 * @param <T> Tag class to be serialized
 */
public final class TagTypeAdapter<E extends Keyed> extends TypeAdapter<Tag<E>> {
    private final TypeAdapter<String> stringAdapter;
    private final String registry;
    private final Class<E> elementType;

    public TagTypeAdapter(Gson gson, String registry, Class<E> elementType) {
        this.stringAdapter = gson.getAdapter(String.class);
        this.registry = registry;
        this.elementType = elementType;
    }

    @Override
    public void write(JsonWriter out, Tag<E> value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        stringAdapter.write(out, value.getKey().toString());
    }

    @Override
    public Tag<E> read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String key = stringAdapter.read(in);
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        if (namespacedKey == null) {
            throw new JsonParseException("Invalid tag key format: " + key);
        }

        Tag<E> tag = Bukkit.getTag(registry, namespacedKey, elementType);
        if (tag == null) {
            throw new JsonParseException("Unknown tag: " + key);
        }

        return tag;
    }
}