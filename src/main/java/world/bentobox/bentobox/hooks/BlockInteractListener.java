package world.bentobox.bentobox.hooks;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;

/**
 * Listens for changes to the ItemsAdder flag
 */
public class BlockInteractListener extends FlagListener {

    /**
     * Handles explosions of ItemAdder items
     * @param event explosion event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!EntityType.PLAYER.equals(event.getEntityType())) {
            // Ignore non-player explosions.
            return;
        }

        Player player = (Player) event.getEntity();

        if (!player.hasPermission("XXXXXX")) {
            // Ignore players that does not have magic XXXXXX permission.
            return;
        }

        // Use BentoBox flag processing system to validate usage.
        // Technically not necessary as internally it should be cancelled by BentoBox.

        if (!this.checkIsland(event, player, event.getLocation(), ItemsAdderHook.ITEMS_ADDER_EXPLOSIONS)) {
            // Remove any blocks from the explosion list if required
            event.blockList().removeIf(block -> this.protect(player, block.getLocation()));
            event.setCancelled(this.protect(player, event.getLocation()));
        }
    }

    /**
     * This method returns if the protection in given location is enabled or not.
     * @param player Player who triggers explosion.
     * @param location Location where explosion happens.
     * @return {@code true} if location is protected, {@code false} otherwise.
     */
    private boolean protect(Player player, Location location) {
        return BentoBox.getInstance().getIslands().getProtectedIslandAt(location)
                .map(island -> !island.isAllowed(User.getInstance(player), ItemsAdderHook.ITEMS_ADDER_EXPLOSIONS))
                .orElse(false);
    }
}