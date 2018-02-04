package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.configuration.Adapter;
import us.tastybento.bskyblock.api.flags.Flag;

/**
 * Serializes the {@link us.tastybento.bskyblock.database.objects.Island#getFlags() getFlags()} and 
 * {@link us.tastybento.bskyblock.database.objects.Island#setFlags() setFlags()}
 * in {@link us.tastybento.bskyblock.database.objects.Island}
 * @author tastybento
 *
 */
public class FlagSerializer implements Adapter<HashMap<Flag, Integer>, HashMap<String, Integer>> {

    @Override
    public HashMap<Flag, Integer> convertFrom(Object from) {
        HashMap<Flag, Integer> result = new HashMap<>();
        MemorySection section = (MemorySection) from;
        for (String key : section.getKeys(false)) {
            Bukkit.getLogger().info("DEBUG: " + key + " = " + section.getInt(key));
            
            result.put(BSkyBlock.getInstance().getFlagsManager().getFlagByID(key), section.getInt(key));
        }
        return result;
    }

    @Override
    public HashMap<String, Integer> convertTo(Object to) {
        HashMap<String, Integer> result = new HashMap<>();
        HashMap<Flag, Integer> flags = (HashMap<Flag, Integer>)to;
        for (Entry<Flag, Integer> en: flags.entrySet()) {
            result.put(en.getKey().getID(), en.getValue());
        }
        return result;
    }



}
