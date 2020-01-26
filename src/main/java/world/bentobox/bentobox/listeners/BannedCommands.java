package world.bentobox.bentobox.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Blocks command usage for various scenarios
 * @author tastybento
 *
 */
public class BannedCommands implements Listener {

    private BentoBox plugin;

    /**
     * @param plugin - plugin
     */
    public BannedCommands(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevents visitors from using commands on islands, like /spawner
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVisitorCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getIWM().inWorld(e.getPlayer().getLocation()) || e.getPlayer().isOp()
                || e.getPlayer().hasPermission(plugin.getIWM().getPermissionPrefix(e.getPlayer().getWorld()) + "mod.bypassprotect")
                || plugin.getIslands().locationIsOnIsland(e.getPlayer(), e.getPlayer().getLocation())) {
            return;
        }
        World w = e.getPlayer().getWorld();
        // Check banned commands
        String[] args = e.getMessage().substring(1).toLowerCase(java.util.Locale.ENGLISH).split(" ");
        if (plugin.getIWM().getVisitorBannedCommands(w).contains(args[0])) {
            User user = User.getInstance(e.getPlayer());
            user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation("protection.command-is-banned"));
            e.setCancelled(true);
        }
    }
    
    /**
     * Prevents falling players from using commands, like /warp
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFallingCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getIWM().inWorld(e.getPlayer().getLocation()) || e.getPlayer().isOp()
                || e.getPlayer().hasPermission(plugin.getIWM().getPermissionPrefix(e.getPlayer().getWorld()) + "mod.bypassprotect")
                || !Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(e.getPlayer().getWorld())) {
            return;
        }
        World w = e.getPlayer().getWorld();
        // Check banned commands
        String[] args = e.getMessage().substring(1).toLowerCase(java.util.Locale.ENGLISH).split(" ");
        if (plugin.getIWM().getFallingBannedCommands(w).contains(args[0]) && e.getPlayer().getFallDistance() > 0) {
            User user = User.getInstance(e.getPlayer());
            user.notify(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            e.setCancelled(true);
        }
    }
}
