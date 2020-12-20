package world.bentobox.bentobox.database.json.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectTypeAdapter extends TypeAdapter<PotionEffectType> {

  @Override
  public void write(JsonWriter out, PotionEffectType value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.value(value.getName());
  }

  @Override
  public PotionEffectType read(JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull();
      return null;
    }
    return PotionEffectType.getByName(reader.nextString());
  }
}
