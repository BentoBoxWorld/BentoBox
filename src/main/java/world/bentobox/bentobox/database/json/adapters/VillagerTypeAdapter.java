package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.entity.Villager;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class VillagerTypeAdapter extends TypeAdapter<Villager.Type> {

    @Override
    public void write(JsonWriter out, Villager.Type type) throws IOException {
        if (type == null) {
            out.nullValue();
            return;
        }
        out.value(type.name());
    }

    @Override
    public Villager.Type read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String id = reader.nextString();
        try {
            return Villager.Type.valueOf(id);
        } catch (Exception e) {
            // Do nothing
        }
        return Villager.Type.PLAINS;

    }
}