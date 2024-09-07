package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.entity.Villager.Profession;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ProfessionTypeAdapter extends TypeAdapter<Profession> {

    @Override
    public void write(JsonWriter out, Profession profession) throws IOException {
        if (profession != null) {
            out.value(profession.name());
            return;
        }
        out.nullValue();
    }

    @Override
    public Profession read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String id = reader.nextString();
        try {
            return Profession.valueOf(id);
        } catch (Exception e) {
            // Do nothing
        }
        return Profession.NONE;

    }
}