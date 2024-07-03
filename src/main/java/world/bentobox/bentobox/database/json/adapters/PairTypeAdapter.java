package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import world.bentobox.bentobox.util.Pair;

// Custom TypeAdapter for Pair<X, Z>
public class PairTypeAdapter<X, Z> extends TypeAdapter<Pair<X, Z>> {

    @Override
    public void write(JsonWriter out, Pair<X, Z> pair) throws IOException {
        if (pair == null || pair.getKey() == null || pair.getValue() == null) {
            return;
        }
        out.beginArray();
        out.value(new Gson().toJson(pair.getKey()));
        out.value(new Gson().toJson(pair.getValue()));
        out.endArray();
    }

    @Override
    public Pair<X, Z> read(JsonReader in) throws IOException {
        in.beginArray();
        Type typeX = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        X x = new Gson().fromJson(in.nextString(), typeX);
        Type typeZ = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
        Z z = new Gson().fromJson(in.nextString(), typeZ);
        in.endArray();
        if (x == null || z == null) {
            return null;
        }
        return new Pair<>(x, z);
    }
}