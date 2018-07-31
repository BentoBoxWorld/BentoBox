package world.bentobox.bentobox.database.objects.adapters;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.potion.PotionEffectType;

public class PotionEffectListAdapter implements AdapterInterface<List<PotionEffectType>, List<String>> {

    @SuppressWarnings("unchecked")
    @Override
    public List<PotionEffectType> deserialize(Object from) {
        List<PotionEffectType> result = new ArrayList<>();
        if (from instanceof ArrayList) {
            for (String type: (ArrayList<String>)from) {
                result.add(PotionEffectType.getByName(type));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> serialize(Object to) {
        List<String> result = new ArrayList<>();
        if (to instanceof ArrayList) {
            for (PotionEffectType type: (ArrayList<PotionEffectType>)to) {
                result.add(type.getName());
            }
        }
        return result;
    }

}
