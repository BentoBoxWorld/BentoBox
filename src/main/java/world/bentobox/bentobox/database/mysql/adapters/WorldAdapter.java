package world.bentobox.bentobox.database.mysql.adapters;

import java.io.IOException;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class WorldAdapter extends TypeAdapter<World> {

    private Plugin plugin;

    public WorldAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

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
        return plugin.getServer().getWorld(reader.nextString());
    }
}