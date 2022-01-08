package world.bentobox.bentobox.database.json;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.json.adapters.*;
import world.bentobox.bentobox.versions.ServerCompatibility;


/**
 * Allocates type adapters based on class type.
 *
 * @author tastybento
 *
 */
public class BentoboxTypeAdapterFactory implements TypeAdapterFactory {

    final BentoBox plugin;

    /**
     * @param plugin plugin
     */
    public BentoboxTypeAdapterFactory(BentoBox plugin) {
        this.plugin = plugin;
    }

    /* (non-Javadoc)
     * @see com.google.gson.TypeAdapterFactory#create(com.google.gson.Gson, com.google.gson.reflect.TypeToken)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        if (Location.class.isAssignableFrom(rawType)) {
            // Use our current location adapter for backward compatibility
            return (TypeAdapter<T>) new LocationTypeAdapter();
        } else if (Biome.class.isAssignableFrom(rawType) && !ServerCompatibility.getInstance().isVersion(ServerCompatibility.ServerVersion.V1_17_1)) { // TODO: Any better way ?
            return (TypeAdapter<T>) new BiomeTypeAdapter();
        } else if (Enum.class.isAssignableFrom(rawType)) {
            return new EnumTypeAdapter(rawType);
        } else if (ItemStack.class.isAssignableFrom(rawType)) {
            // Use our current location adapter for backward compatibility
            return (TypeAdapter<T>) new ItemStackTypeAdapter();
        } else if (Flag.class.isAssignableFrom(rawType)) {
            return (TypeAdapter<T>) new FlagTypeAdapter(plugin);
        } else if (PotionEffectType.class.isAssignableFrom(rawType)) {
            return (TypeAdapter<T>) new PotionEffectTypeAdapter();
        } else if (World.class.isAssignableFrom(rawType)) {
            return (TypeAdapter<T>) new WorldTypeAdapter();
        } else if (Vector.class.isAssignableFrom(rawType)) {
            return (TypeAdapter<T>) new VectorTypeAdapter();
        } else if (ConfigurationSerializable.class.isAssignableFrom(rawType)) {
            // This covers a lot of Bukkit objects
            return (TypeAdapter<T>) new BukkitObjectTypeAdapter(gson.getAdapter(Map.class));
        }
        return null;
    }

}
