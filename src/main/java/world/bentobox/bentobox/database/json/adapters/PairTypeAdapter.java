package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.util.Pair;

public class PairTypeAdapter<X, Z> extends TypeAdapter<Pair<X, Z>> {
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
        Gson gson = new Gson();
        gson.toJson(pair.getKey(), xType, out);
        out.name("z");
        gson.toJson(pair.getValue(), zType, out);
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
                x = new Gson().fromJson(in, xType);
            } else if (name.equals("z")) {
                z = new Gson().fromJson(in, zType);
            }
        }
        in.endObject();
        return new Pair<>(x, z);
    }
}
