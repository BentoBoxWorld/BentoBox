package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;

import org.bukkit.util.BoundingBox;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class BoundingBoxTypeAdapter extends TypeAdapter<BoundingBox> {

    @Override
    public void write(JsonWriter out, BoundingBox box) throws IOException {
        if (box == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        out.value(box.getMinX());
        out.value(box.getMinY());
        out.value(box.getMinZ());
        out.value(box.getMaxX());
        out.value(box.getMaxY());
        out.value(box.getMaxZ());
        out.endArray();
    }

    @Override
    public BoundingBox read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        in.beginArray();
        double minX = in.nextDouble();
        double minY = in.nextDouble();
        double minZ = in.nextDouble();
        double maxX = in.nextDouble();
        double maxY = in.nextDouble();
        double maxZ = in.nextDouble();
        in.endArray();
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}