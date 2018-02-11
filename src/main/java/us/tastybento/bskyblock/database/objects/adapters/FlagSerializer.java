package us.tastybento.bskyblock.database.objects.adapters;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;

/**
 * Serializes the {@link us.tastybento.bskyblock.database.objects.Island#getFlags() getFlags()} and
 * {@link us.tastybento.bskyblock.database.objects.Island#setFlags() setFlags()}
 * in {@link us.tastybento.bskyblock.database.objects.Island}
 * @author tastybento
 *
 */
public class FlagSerializer implements AdapterInterface<HashMap<Flag, Integer>, HashMap<String, Integer>> {

    @Override
    public HashMap<Flag, Integer> serialize(Object object) {
        HashMap<Flag, Integer> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        // For YAML
        if (object instanceof MemorySection) {
            MemorySection section = (MemorySection) object;
            for (String key : section.getKeys(false)) {
                Bukkit.getLogger().info("DEBUG: " + key + " = " + section.getInt(key));

                result.put(BSkyBlock.getInstance().getFlagsManager().getFlagByID(key), section.getInt(key));
            }
        } else {
            for (Entry<String, Integer> en : ((HashMap<String, Integer>)object).entrySet()) {
                result.put(BSkyBlock.getInstance().getFlagsManager().getFlagByID(en.getKey()), en.getValue());
            }
        }
        return result;
    }

    @Override
    public HashMap<String, Integer> deserialize(Object object) {
        HashMap<String, Integer> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        HashMap<Flag, Integer> flags = (HashMap<Flag, Integer>)object;
        for (Entry<Flag, Integer> en: flags.entrySet()) {
            result.put(en.getKey().getID(), en.getValue());
        }
        return result;
    }



}
