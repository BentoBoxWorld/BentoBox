package world.bentobox.bentobox.database.json.adapters;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.bukkit.Fluid;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class TagTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        if (Tag.class.isAssignableFrom(rawType)) {
            // Get the generic type parameter of Tag<T>
            Type[] typeArguments = ((ParameterizedType) type.getType()).getActualTypeArguments();
            if (typeArguments.length > 0) {
                Class<? extends Keyed> tagType = (Class<? extends Keyed>) typeArguments[0];
                
                // Determine the correct registry based on the tag type
                String registry;
                if (Material.class.equals(tagType)) {
                    registry = Tag.REGISTRY_BLOCKS;
                } else if (EntityType.class.equals(tagType)) {
                    registry = Tag.REGISTRY_ENTITY_TYPES;
                } else if (Fluid.class.equals(tagType)) {
                    registry = Tag.REGISTRY_FLUIDS;
                } else {
                    throw new IllegalArgumentException("Unsupported Tag type: " + tagType);
                }
                
                return (TypeAdapter<T>) new TagTypeAdapter(gson, registry, tagType);
            }
        }
        return null;
    }
}