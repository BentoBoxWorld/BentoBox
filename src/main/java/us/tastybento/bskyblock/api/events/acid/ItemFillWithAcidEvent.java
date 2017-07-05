package us.tastybento.bskyblock.api.events.acid;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when an ItemStack (water bottle or bucket) is filled with acid
 * @author Poslovitch
 * @since 1.0
 */
public class ItemFillWithAcidEvent extends IslandEvent {
    private final Player player;
    private final ItemStack item;

    // TODO: dispenser?
    public ItemFillWithAcidEvent(Island island, Player player, ItemStack item) {
        super(island);
        this.player = player;
        this.item = item;
    }

    /**
     * Gets the player who triggered the event
     * @return the player who triggered the event
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the item that will be acid-ified
     * @return the item that will be acid-ified
     */
    public ItemStack getItem() {
        return item;
    }
}
