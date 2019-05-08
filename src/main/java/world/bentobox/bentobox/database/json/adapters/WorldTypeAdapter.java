package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class WorldTypeAdapter extends TypeAdapter<World> {

    @Override
    public void write(JsonWriter out, World value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.getName());

    }

    @Override
    public World read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        return Bukkit.getServer().getWorld(reader.nextString());
    }
}