package world.bentobox.bentobox.database.objects.adapters;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.MemorySection;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * Serializes the {@link world.bentobox.bentobox.database.objects.Island#getFlags() getFlags()} and
 * {@link world.bentobox.bentobox.database.objects.Island#setFlags(Map)} () setFlags()}
 * in {@link world.bentobox.bentobox.database.objects.Island}
 * @author tastybento
 * @since 1.6.0
 */
public class FlagSerializer3 implements AdapterInterface<Map<Flag, Long>, Map<String, Long>> {

    @SuppressWarnings("unchecked")
    @Override
    public Map<Flag, Long> deserialize(Object object) {
        Map<Flag, Long> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        // For YAML
        if (object instanceof MemorySection) {
            MemorySection section = (MemorySection) object;
            for (String key : section.getKeys(false)) {
                BentoBox.getInstance().getFlagsManager().getFlag(key).ifPresent(flag -> result.put(flag, section.getLong(key)));
            }
        } else {
            for (Entry<String, Long> en : ((Map<String, Long>)object).entrySet()) {
                BentoBox.getInstance().getFlagsManager().getFlag(en.getKey()).ifPresent(flag -> result.put(flag, en.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Long> serialize(Object object) {
        Map<String, Long> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        Map<Flag, Long> flags = (Map<Flag, Long>)object;
        for (Entry<Flag, Long> en: flags.entrySet()) {
            if (en != null && en.getKey() != null) {
                result.put(en.getKey().getID(), en.getValue());
            }
        }
        return result;
    }
}
