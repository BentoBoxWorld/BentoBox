package world.bentobox.bentobox.listeners;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class JoinLeaveListener implements Listener {

    private BentoBox plugin;
    private PlayersManager players;

    /**
     * @param plugin - plugin object
     */
    public JoinLeaveListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        players = plugin.getPlayers();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Remove them from the cache, just in case they were not removed for some reason
        User.removePlayer(event.getPlayer());

        User user = User.getInstance(event.getPlayer());
        if (user == null || user.getUniqueId() == null) {
            return;
        }
        UUID playerUUID = user.getUniqueId();

        // Check if player hasn't joined before
        if (!players.isKnown(playerUUID)) {
            firstTime(user);
        }

        // Make sure the player is loaded into the cache or create the player if they don't exist
        players.addPlayer(playerUUID);

        // Reset island resets if required
        plugin.getIWM().getOverWorlds().stream()
        .filter(w -> event.getPlayer().getLastPlayed() < plugin.getIWM().getResetEpoch(w))
        .forEach(w -> players.setResets(w, playerUUID, 0));

        // Automated island ownership transfer
        if (plugin.getSettings().isEnableAutoOwnershipTransfer()) {
            runAutomatedOwnershipTransfer(user);
        }

        // Update the island range of the islands the player owns
        updateIslandRange(user);

        // Set the player's name (it may have changed), but only if it isn't empty
        if (!user.getName().isEmpty()) {
            players.setPlayerName(user);
            players.save(playerUUID);
        } else {
            plugin.logWarning("Player that just logged in has no name! " + playerUUID.toString());
        }

        // If mobs have to be removed when a player joins, then wipe all the mobs on his island.
        if (plugin.getIslands().locationIsOnIsland(event.getPlayer(), user.getLocation()) && Flags.REMOVE_MOBS.isSetForWorld(user.getWorld())) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getIslands().clearArea(user.getLocation()));
        }

        // Clear inventory if required
        clearPlayersInventory(Util.getWorld(event.getPlayer().getWorld()), user);
    }


    private void firstTime(User user) {
        // Make sure the player is loaded into the cache or create the player if they don't exist
        players.addPlayer(user.getUniqueId());

        plugin.getIWM().getOverWorlds().stream()
        .filter(w -> plugin.getIWM().isCreateIslandOnFirstLoginEnabled(w))
        .forEach(w -> {
            // Even if that'd be extremely unlikely, it's better to check if the player doesn't have an island already.
            if (!(plugin.getIslands().hasIsland(w, user) || plugin.getIslands().inTeam(w, user.getUniqueId()))) {
                int delay = plugin.getIWM().getCreateIslandOnFirstLoginDelay(w);
                user.sendMessage("commands.island.create.on-first-login",
                        TextVariables.NUMBER, String.valueOf(delay));

                Runnable createIsland = () -> {
                    // should only execute if:
                    // - abort on logout is false
                    // - abort on logout is true && user is online
                    if (!plugin.getIWM().isCreateIslandOnFirstLoginAbortOnLogout(w) || user.isOnline()){
                        plugin.getIWM().getAddon(w).ifPresent(addon -> addon.getPlayerCommand()
                                .map(command -> command.getSubCommand("create").orElse(null))
                                .ifPresent(command -> command.execute(user, "create", Collections.singletonList(BlueprintsManager.DEFAULT_BUNDLE_NAME))));
                    }
                };

                if (delay <= 0) {
                    Bukkit.getScheduler().runTask(plugin, createIsland);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, createIsland, delay * 20L);
                }
            }
        });

    }

    /**
     * This event will clean players inventor
     * @param event SwitchWorld event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSwitchWorld(final PlayerChangedWorldEvent event) {
        World world = Util.getWorld(event.getPlayer().getWorld());
        // Clear inventory if required
        if (world != null) {
            clearPlayersInventory(world, User.getInstance(event.getPlayer()));
        }
    }


    /**
     * This method clears player inventory and ender chest if given world is quarantined
     * in user data file and it is required by plugin settings.
     * @param world World where cleaning must occur.
     * @param user Targeted user.
     */
    private void clearPlayersInventory(World world, @NonNull User user) {
        if (user.getUniqueId() == null) return;
        // Clear inventory if required
        Players playerData = players.getPlayer(user.getUniqueId());

        if (playerData != null && playerData.getPendingKicks().contains(world.getName())) {
            if (plugin.getIWM().isOnLeaveResetEnderChest(world)) {
                user.getPlayer().getEnderChest().clear();
            }

            if (plugin.getIWM().isOnLeaveResetInventory(world)) {
                user.getPlayer().getInventory().clear();
            }

            playerData.getPendingKicks().remove(world.getName());
            players.save(user.getUniqueId());
        }
    }


    private void runAutomatedOwnershipTransfer(User user) {
        plugin.getIWM().getOverWorlds().stream()
        .filter(world -> plugin.getIslands().hasIsland(world, user) && !plugin.getIslands().isOwner(world, user.getUniqueId()))
        .forEach(world -> {
            Island island = plugin.getIslands().getIsland(world, user);

            OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());

            // Converting the setting (in days) to milliseconds.
            long inactivityThreshold = plugin.getSettings().getAutoOwnershipTransferInactivityThreshold() * 24 * 60 * 60 * 1000L;
            long timestamp = System.currentTimeMillis() - inactivityThreshold;

            // We make sure the current owner is inactive.
            if (owner.getLastPlayed() != 0 && owner.getLastPlayed() < timestamp) {
                // The current owner is inactive
                // Now, let's run through all of the island members (except the player who's just joined) and see who's active.
                // Sadly, this will make us calculate the owner inactivity again... :(
                List<UUID> candidates = Arrays.asList((UUID[]) island.getMemberSet().stream()
                        .filter(uuid -> !user.getUniqueId().equals(uuid))
                        .filter(uuid -> Bukkit.getOfflinePlayer(uuid).getLastPlayed() != 0
                        && Bukkit.getOfflinePlayer(uuid).getLastPlayed() < timestamp)
                        .toArray());

                if (!candidates.isEmpty() && !plugin.getSettings().isAutoOwnershipTransferIgnoreRanks()) {
                    // Ranks are not ignored, our candidates can only have the highest rank
                    // TODO Complete this section
                }
            }
        });
    }

    private void updateIslandRange(User user) {
        plugin.getIWM().getOverWorlds().stream()
        .filter(world -> plugin.getIslands().isOwner(world, user.getUniqueId()))
        .forEach(world -> {
            Island island = plugin.getIslands().getIsland(world, user);
            if (island != null) {
                // Check if new owner has a different range permission than the island size
                int range = user.getPermissionValue(plugin.getIWM().getAddon(island.getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("") + "island.range", island.getProtectionRange());
                // Range cannot be greater than the island distance
                range = Math.min(range, plugin.getIWM().getIslandDistance(island.getWorld()));
                // Range can go up or down
                if (range != island.getProtectionRange()) {
                    user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                    plugin.log("Island protection range changed from " + island.getProtectionRange() + " to "
                            + range + " for " + user.getName() + " due to permission.");

                    // Get old range for event
                    int oldRange = island.getProtectionRange();

                    island.setProtectionRange(range);

                    // Call Protection Range Change event. Does not support cancelling.
                    IslandEvent.builder()
                    .island(island)
                    .location(island.getCenter())
                    .reason(IslandEvent.Reason.RANGE_CHANGE)
                    .involvedPlayer(user.getUniqueId())
                    .admin(true)
                    .protectionRange(range, oldRange)
                    .build();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        // Remove any coops if all the island players have left
        plugin.getIWM().getOverWorlds().forEach(w -> {
            Island island = plugin.getIslands().getIsland(w, User.getInstance(event.getPlayer()));
            // Are there any online players still for this island?
            if (island != null && Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !event.getPlayer().equals(p))
                    .noneMatch(p -> plugin.getIslands().getMembers(w, event.getPlayer().getUniqueId()).contains(p.getUniqueId()))) {
                // No, there are no more players online on this island
                // Tell players they are being removed
                island.getMembers().entrySet().stream()
                .filter(e -> e.getValue() == RanksManager.COOP_RANK)
                .forEach(e -> User.getInstance(e.getKey())
                        .sendMessage("commands.island.team.uncoop.all-members-logged-off", TextVariables.NAME, plugin.getPlayers().getName(island.getOwner())));
                // Remove any coop players on this island
                island.removeRank(RanksManager.COOP_RANK);
            }
        });
        // Remove any coop associations from the player logging out
        plugin.getIslands().clearRank(RanksManager.COOP_RANK, event.getPlayer().getUniqueId());
        players.save(event.getPlayer().getUniqueId());
        User.removePlayer(event.getPlayer());
    }
}
