package world.bentobox.bentobox.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

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

            // Update the island range of the islands the player owns
            plugin.getIWM().getOverWorlds().stream()
                    .filter(world -> plugin.getIslands().isOwner(world, user.getUniqueId()))
                    .forEach(world -> {
                        Island island = plugin.getIslands().getIsland(world, user);

                        // Check if new leader has a different range permission than the island size
                        int range = user.getPermissionValue(plugin.getIWM().getAddon(island.getWorld()).get().getPermissionPrefix() + "island.range", plugin.getIWM().getIslandProtectionRange(Util.getWorld(island.getWorld())));

                        // Range can go up or down
                        if (range != island.getProtectionRange()) {
                            user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                            plugin.log("Makeleader: Island protection range changed from " + island.getProtectionRange() + " to "
                                    + range + " for " + user.getName() + " due to permission.");
                        }

                        island.setProtectionRange(range);
                    });

            // Set the player's name (it may have changed), but only if it isn't empty
            if (!user.getName().isEmpty()) {
                players.setPlayerName(user);
                players.save(playerUUID);
            } else {
                plugin.logWarning("Player that just logged in has no name! " + playerUUID.toString());
            }

            // If mobs have to be removed when a player joins, then wipe all the mobs on his island.
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
