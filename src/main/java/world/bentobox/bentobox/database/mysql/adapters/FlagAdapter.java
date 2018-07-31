package world.bentobox.bentobox.database.mysql.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;

public class FlagAdapter extends TypeAdapter<Flag> {

    private BentoBox plugin;

    public FlagAdapter(BentoBox plugin) {
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
        return plugin.getFlagsManager().getFlagByID(reader.nextString());
    }
}