package world.bentobox.bentobox.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Blocks visitors from executing commands that they should not in the island world
 * @author tastybento
 *
 */
public class BannedVisitorCommands implements Listener {

    private BentoBox plugin;

    /**
     * @param plugin - plugin
     */
    public BannedVisitorCommands(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevents visitors from using commands on islands, like /spawner
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVisitorCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getIWM().inWorld(e.getPlayer().getLocation()) || e.getPlayer().isOp()
                || e.getPlayer().hasPermission(plugin.getIWM().getPermissionPrefix(e.getPlayer().getWorld()) + "mod.bypassprotect")
                || plugin.getIslands().locationIsOnIsland(e.getPlayer(), e.getPlayer().getLocation())) {
            return;
        }
        // Check banned commands
        String[] args = e.getMessage().substring(1).toLowerCase(java.util.Locale.ENGLISH).split(" ");
        if (plugin.getIWM().getVisitorBannedCommands(e.getPlayer().getWorld()).contains(args[0])) {
            User user = User.getInstance(e.getPlayer());
            user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation("protection.command-is-banned"));
            e.setCancelled(true);
        }
    }
}
