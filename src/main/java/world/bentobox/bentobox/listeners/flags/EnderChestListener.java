/*

 */
package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents enderchest use and creation in world if it is not allowed
 * @author tastybento
 *
 */
public class EnderChestListener extends AbstractFlagListener {

    /**
     * Prevents opening ender chest unless player has permission
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEnderChestOpen(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {          
            e.setCancelled(checkEnderChest(e.getPlayer(), e.getClickedBlock().getType()));
        }
    }
    
    /**
     * Prevents crafting of EnderChest unless the player has permission
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        e.setCancelled(checkEnderChest((Player)e.getWhoClicked(), e.getRecipe().getResult().getType()));
    }

    private boolean checkEnderChest(Player player, Material type) {
        if (type.equals(Material.ENDER_CHEST)
                && getIWM().inWorld(player.getLocation())
                && !player.isOp()
                && !player.hasPermission(getPlugin().getIWM().getPermissionPrefix(player.getWorld()) + ".craft.enderchest")
                && !Flags.ENDER_CHEST.isSetForWorld(player.getWorld())) {
            // Not allowed
            User user = User.getInstance(player);
            user.sendMessage("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(Flags.ENDER_CHEST.getHintReference()));
            return true;
        }
        return false;
    }
}
