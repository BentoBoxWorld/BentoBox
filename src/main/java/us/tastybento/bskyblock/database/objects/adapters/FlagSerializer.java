package us.tastybento.bskyblock.database.objects.adapters;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.MemorySection;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;

/**
 * Serializes the {@link us.tastybento.bskyblock.database.objects.Island#getFlags() getFlags()} and
 * {@link us.tastybento.bskyblock.database.objects.Island#setFlags(Map)} () setFlags()}
 * in {@link us.tastybento.bskyblock.database.objects.Island}
 * @author tastybento
 *
 */
public class FlagSerializer implements AdapterInterface<Map<Flag, Integer>, Map<String, Integer>> {

    @SuppressWarnings("unchecked")
    @Override
    public Map<Flag, Integer> serialize(Object object) {
        Map<Flag, Integer> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        // For YAML
        if (object instanceof MemorySection) {
            MemorySection section = (MemorySection) object;
            for (String key : section.getKeys(false)) {
                result.put(BSkyBlock.getInstance().getFlagsManager().getFlagByID(key), section.getInt(key));
            }
        } else {
            for (Entry<String, Integer> en : ((Map<String, Integer>)object).entrySet()) {
                result.put(BSkyBlock.getInstance().getFlagsManager().getFlagByID(en.getKey()), en.getValue());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Integer> deserialize(Object object) {
        Map<String, Integer> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        Map<Flag, Integer> flags = (Map<Flag, Integer>)object;
        for (Entry<Flag, Integer> en: flags.entrySet()) {
            result.put(en.getKey().getID(), en.getValue());
        }
        return result;
    }
}
