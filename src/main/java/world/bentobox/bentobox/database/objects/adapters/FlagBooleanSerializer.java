package world.bentobox.bentobox.database.objects.adapters;


import org.bukkit.configuration.MemorySection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * This Serializer migrates Map of String, Boolean to Map of String, Integer in serialization process.
 * It is necessary because current implementation requires flags to be mapped to Integer value.
 * @author BONNe
 */
public class FlagBooleanSerializer implements AdapterInterface<Map<String, Integer>, Map<String, Boolean>>
{
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Integer> deserialize(Object object)
    {
        Map<String, Integer> result = new HashMap<>();
        if (object == null)
        {
            return result;
        }
        // For YAML
        if (object instanceof MemorySection section)
        {
            for (String key : section.getKeys(false))
            {
                result.put(key, section.getBoolean(key) ? 0 : -1);
            }
        }
        else
        {
            for (Entry<String, Boolean> en : ((Map<String, Boolean>) object).entrySet())
            {
                result.put(en.getKey(), en.getValue() ? 0 : -1);
            }
        }

        return result;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Boolean> serialize(Object object)
    {
        Map<String, Boolean> result = new HashMap<>();

        if (object == null)
        {
            return result;
        }

        Map<String, Integer> flags = (Map<String, Integer>) object;

        for (Entry<String, Integer> en : flags.entrySet())
        {
            result.put(en.getKey(), en.getValue() >= 0);
        }
        return result;
    }
}
