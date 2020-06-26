package world.bentobox.bentobox.util.heads;

import org.bukkit.inventory.ItemStack;

/**
 * @since 1.14.0
 * @author tastybento
 */
public class HeadCache {
    private final ItemStack head;
    private final long timestamp;
    /**
     * @param head
     * @param timestamp
     */
    public HeadCache(ItemStack head, long timestamp) {
        super();
        this.head = head;
        this.timestamp = timestamp;
    }
    /**
     * @return the head
     */
    public ItemStack getHead() {
        return head;
    }
    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }


}
