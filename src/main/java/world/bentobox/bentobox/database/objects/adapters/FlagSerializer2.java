package world.bentobox.bentobox.database.objects.adapters;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.MemorySection;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * @author tastybento
 *
 */
public class FlagSerializer2 implements AdapterInterface<Map<Flag, Integer>, Map<String, Boolean>> {

    @SuppressWarnings("unchecked")
    @Override
    public Map<Flag, Integer> deserialize(Object object) {
        Map<Flag, Integer> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        // For YAML
        if (object instanceof MemorySection) {
            MemorySection section = (MemorySection) object;
            for (String key : section.getKeys(false)) {
                result.put(BentoBox.getInstance().getFlagsManager().getFlagByID(key), section.getBoolean(key) ? 0 : -1);
            }
        } else {
            for (Entry<String, Boolean> en : ((Map<String, Boolean>)object).entrySet()) {
                result.put(BentoBox.getInstance().getFlagsManager().getFlagByID(en.getKey()), en.getValue() ? 0 : -1);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Boolean> serialize(Object object) {
        Map<String, Boolean> result = new HashMap<>();
        if (object == null) {
            return result;
        }
        Map<Flag, Integer> flags = (Map<Flag, Integer>)object;
        for (Entry<Flag, Integer> en: flags.entrySet()) {
            result.put(en.getKey().getID(), en.getValue() >= 0);
        }
        return result;
    }
}
