package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.util.Pair;

public class PairTypeAdapter<X, Z> extends TypeAdapter<Pair<X, Z>> {
    // Gson construction is expensive and this adapter runs per field on every database load
    private static final Gson GSON = new Gson();

    private final Type xType;
    private final Type zType;

    public PairTypeAdapter(Type xType, Type zType) {
        this.xType = xType;
        this.zType = zType;
    }

    @Override
    public void write(JsonWriter out, Pair<X, Z> pair) throws IOException {
        out.beginObject();
        out.name("x");
        GSON.toJson(pair.getKey(), xType, out);
        out.name("z");
        GSON.toJson(pair.getValue(), zType, out);
        out.endObject();
    }

    @Override
    public Pair<X, Z> read(JsonReader in) throws IOException {
        X x = null;
        Z z = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (name.equals("x")) {
                x = GSON.fromJson(in, xType);
            } else if (name.equals("z")) {
                z = GSON.fromJson(in, zType);
            }
        }
        in.endObject();
        return new Pair<>(x, z);
    }
}
