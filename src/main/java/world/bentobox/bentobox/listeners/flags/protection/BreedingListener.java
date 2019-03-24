package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

import java.util.Arrays;
import java.util.List;

/**
 * Handles breeding protection
 * Note - animal protection is done elsewhere.
 * @author tastybento
 *
 */
public class BreedingListener extends FlagListener {

    /**
     * A list of items that cause breeding if a player has them in their hand and they click an animal
     * This list may need to be extended with future versions of Minecraft.
     */
    private static final List<Material> BREEDING_ITEMS = Arrays.asList(
            Material.EGG,
            Material.WHEAT,
            Material.CARROT,
            Material.WHEAT_SEEDS,
            Material.SEAGRASS);

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof Animals) {
            ItemStack inHand = e.getPlayer().getInventory().getItemInMainHand();
            if (e.getHand().equals(EquipmentSlot.OFF_HAND)) {
                inHand = e.getPlayer().getInventory().getItemInOffHand();
            }
            if (BREEDING_ITEMS.contains(inHand.getType()) && !checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.BREEDING)) {
                ((Animals)e.getRightClicked()).setBreed(false);
            }
        }
    }
}
