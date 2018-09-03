package world.bentobox.bentobox.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.PlayersManager;

public class JoinLeaveListener implements Listener {

    private BentoBox plugin;
    private PlayersManager players;

    /**
     * @param plugin - plugin object
     */
    public JoinLeaveListener(BentoBox plugin) {
        this.plugin = plugin;
        players = plugin.getPlayers();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        User user = User.getInstance(event.getPlayer());
        if (user.getUniqueId() == null) {
            return;
        }
        UUID playerUUID = user.getUniqueId();
        // Load player
        players.addPlayer(playerUUID);
        if (plugin.getPlayers().isKnown(playerUUID)) {
            // Reset island resets if required
            plugin.getIWM().getOverWorlds().stream()
            .filter(w -> event.getPlayer().getLastPlayed() < plugin.getIWM().getResetEpoch(w))
            .forEach(w -> players.setResets(w, playerUUID, 0));
            // Set the player's name (it may have changed), but only if it isn't empty
            if (!user.getName().isEmpty()) {
                players.setPlayerName(user);
                players.save(playerUUID);
            } else {
                plugin.logWarning("Player that just logged in has no name! " + playerUUID.toString());
            }
            if (plugin.getIWM().inWorld(user.getLocation()) && Flags.REMOVE_MOBS.isSetForWorld(user.getWorld())) {
                plugin.getIslands().clearArea(user.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        players.save(event.getPlayer().getUniqueId());
        User.removePlayer(event.getPlayer());
    }
}
