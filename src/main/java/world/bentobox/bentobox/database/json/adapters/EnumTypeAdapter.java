package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.util.Arrays;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * @author tastybento
 *
 * @param <T> enum class to be serialized
 */
public final class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {


    /**
     * Bimap to store name,enum pair references
     */
    private final BiMap<String, T> enumMap = HashBiMap.create();


    public EnumTypeAdapter(Class<T> enumClass) {
        for (T value : enumClass.getEnumConstants()) {

            String name = value.name();
            try {
                SerializedName annotation = enumClass.getField(name).getAnnotation(SerializedName.class);

                if (annotation != null) {
                    Arrays.stream(annotation.alternate()).forEach(s -> enumMap.put(s, value));
                    // Reset name
                    name = annotation.value();
                }

            } catch (NoSuchFieldException e) {
                // Ignore
            }

            enumMap.put(name, value);
        }
    }

    @Override public T read(JsonReader input) throws IOException {
        if (JsonToken.NULL.equals(input.peek())) {
            input.nextNull();
            return null;
        }
        return enumMap.get(input.nextString());
    }

    @Override public void write(JsonWriter output, T enumValue) throws IOException {
        output.value(enumValue != null ? enumMap.inverse().get(enumValue) : null);
    }
}
