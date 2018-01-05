/**
 * 
 */
package us.tastybento.bskyblock;

import org.bukkit.Bukkit;

/**
 * @author ben
 * Takes in inputs and turns them into an enum
 */
public class EnumAdapter {
    
    /**
     * Takes in inputs and turns them into an enum of type
     * @param input
     * @param type
     * @return
     */
    public Object adapt(Object input, Object type) {
        Bukkit.getLogger().info("Ran the EnumAdapter: input is " + input + " and type = " + type);
        return Enum.valueOf((Class)type, (String)input);
    }
    
}
