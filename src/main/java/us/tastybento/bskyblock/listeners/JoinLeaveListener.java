package us.tastybento.bskyblock.listeners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

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
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID playerUUID = player.getUniqueId();
        if (playerUUID == null) {
            return;
        }
        if (plugin.getPlayers().isKnown(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: known player");
            // Load player
            players.addPlayer(playerUUID);
            // Reset resets if the admin changes it to or from unlimited
            if (Settings.resetLimit < players.getResetsLeft(playerUUID)  || (Settings.resetLimit >= 0 && players.getResetsLeft(playerUUID) < 0)) {
                players.setResetsLeft(playerUUID, Settings.resetLimit);
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Setting player's name");
            // Set the player's name (it may have changed), but only if it isn't empty
            if (!player.getName().isEmpty()) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Player name is " + player.getName());
                players.setPlayerName(playerUUID, player.getName());
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Saving player");
                players.save(playerUUID);
            } else {
                plugin.getLogger().warning("Player that just logged in has no name! " + playerUUID.toString());
            }
            if (Settings.removeMobsOnLogin) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Removing mobs");
                plugin.getIslands().removeMobs(player.getLocation());
            }

            // Check if they logged in to a locked island and expel them or if they are banned
            Island currentIsland = plugin.getIslands().getIslandAt(player.getLocation());
            if (currentIsland != null && (currentIsland.isLocked() || plugin.getPlayers().isBanned(currentIsland.getOwner(),player.getUniqueId()))) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Current island is locked, or player is banned");
                if (!currentIsland.getMembers().contains(playerUUID) && !player.hasPermission(Settings.PERMPREFIX + "mod.bypassprotect")) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: No bypass - teleporting");
                    player.sendMessage(plugin.getLocale(player).get("locked.islandlocked"));
                    plugin.getIslands().homeTeleport(player);
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
    }
}
