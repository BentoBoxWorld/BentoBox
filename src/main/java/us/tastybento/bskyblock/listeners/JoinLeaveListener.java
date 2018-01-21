package us.tastybento.bskyblock.listeners;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.objects.Island;

public class JoinLeaveListener implements Listener {

    private static final boolean DEBUG = false;
    private BSkyBlock plugin;
    private PlayersManager players;

    /**
     * @param plugin
     */
    public JoinLeaveListener(BSkyBlock plugin) {
        this.plugin = plugin;
        this.players = plugin.getPlayers();
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
        if (plugin.getPlayers().isKnown(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: known player");
            // Load player
            players.addPlayer(playerUUID);
            // Reset resets if the admin changes it to or from unlimited
            if (plugin.getSettings().getResetLimit() < players.getResetsLeft(playerUUID)  || (plugin.getSettings().getResetLimit() >= 0 && players.getResetsLeft(playerUUID) < 0)) {
                players.setResetsLeft(playerUUID, plugin.getSettings().getResetLimit());
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Setting player's name");
            // Set the player's name (it may have changed), but only if it isn't empty
            if (!user.getName().isEmpty()) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Player name is " + user.getName());
                players.setPlayerName(user);
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Saving player");
                players.save(playerUUID);
            } else {
                plugin.getLogger().warning("Player that just logged in has no name! " + playerUUID.toString());
            }
            if (plugin.getSettings().isRemoveMobsOnLogin()) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Removing mobs");
                plugin.getIslands().removeMobs(user.getLocation());
            }

            // Check if they logged in to a locked island and expel them or if they are banned
            Island currentIsland = plugin.getIslands().getIslandAt(user.getLocation());
            if (currentIsland != null && (currentIsland.isLocked() || plugin.getPlayers().isBanned(currentIsland.getOwner(),user.getUniqueId()))) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Current island is locked, or player is banned");
                if (!currentIsland.getMembers().contains(playerUUID) && !user.hasPermission(Constants.PERMPREFIX + "mod.bypassprotect")) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: No bypass - teleporting");
                    user.sendMessage("locked.islandlocked");
                    plugin.getIslands().homeTeleport(user.getPlayer());
                }
            }
        } else {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: not a known player");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        players.removeOnlinePlayer(event.getPlayer().getUniqueId());
        User.removePlayer(event.getPlayer());
    }
}
