package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents players from changing a spawner's entity using spawn eggs.
 * @since 1.7.0
 * @see world.bentobox.bentobox.lists.Flags#SPAWNER_SPAWN_EGGS
 */
public class SpawnerSpawnEggsListener extends FlagListener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawnerChange(final PlayerInteractEvent e) {
        User user = User.getInstance(e.getPlayer());
        // Checking if the clicked block is a spawner and the item in hand is a mob egg
        if (e.getClickedBlock() != null && e.getClickedBlock().getType().equals(Material.SPAWNER)
                && e.getItem() != null && e.getItem().getType().toString().endsWith("_SPAWN_EGG")
                && getIWM().inWorld(e.getClickedBlock().getWorld())
                && !(user.hasPermission(getIWM().getPermissionPrefix(e.getClickedBlock().getWorld()) + "mod.bypass." + Flags.SPAWNER_SPAWN_EGGS.getID() + ".everywhere")
                || user.hasPermission(getIWM().getPermissionPrefix(e.getClickedBlock().getWorld()) + "mod.bypassprotect"))
                && !Flags.SPAWNER_SPAWN_EGGS.isSetForWorld(e.getClickedBlock().getWorld())) {
            // Not allowed
            e.setCancelled(true);
            // Notify the user
            user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(Flags.SPAWNER_SPAWN_EGGS.getHintReference()));
        }
    }
}
