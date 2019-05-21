package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Material;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;

public class FlagTypeAdapter extends TypeAdapter<Flag> {

    private BentoBox plugin;

    public FlagTypeAdapter(BentoBox plugin) {
        this.plugin = plugin;
    }

    @Override
    public void write(JsonWriter out, Flag value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.getID());

    }

    @Override
    public Flag read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String id = reader.nextString();
        Flag f = plugin.getFlagsManager().getFlag(id).orElse(null);
        // Flags can end up null if an addon that created one is removed or if a flag name was changed
        if (f == null) {
            // Create a temporary flag with a unique key. It will be immediately deleted after loading
            f = new Flag.Builder("NULL_FLAG_"+ UUID.randomUUID().toString(), Material.STONE).build();
        }
        return f;
    }
}