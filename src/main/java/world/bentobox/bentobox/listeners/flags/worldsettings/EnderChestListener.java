/*

 */
package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents enderchest creation in world if it is not allowed
 * Enderchest opening is handled in {@link BlockInteractionListener}
 * @author tastybento
 *
 */
public class EnderChestListener extends FlagListener {

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
            user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(Flags.ENDER_CHEST.getHintReference()));
            return true;
        }
        return false;
    }
}
