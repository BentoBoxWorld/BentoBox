package world.bentobox.bentobox.listeners;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class JoinLeaveListener implements Listener {

    private final BentoBox plugin;
    private final PlayersManager players;

    /**
     * @param plugin - plugin object
     */
    public JoinLeaveListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        players = plugin.getPlayers();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Remove them from the cache, just in case they were not removed for some
        // reason
        User.removePlayer(event.getPlayer());

        User user = User.getInstance(event.getPlayer());
        if (!user.isPlayer() || user.getUniqueId() == null) {
            // This should never be the case, but it might be caused by some fake player
            // plugins
            return;
        }
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Check if player hasn't joined before
        if (!players.isKnown(playerUUID)) {
            firstTime(user);
        }

        // Make sure the player is loaded into the cache or create the player if they
        // don't exist
        players.getPlayer(playerUUID);

        // Set the login
        players.setLoginTimeStamp(user);

        // Reset island resets if required
        plugin.getIWM().getOverWorlds().stream()
                .filter(w -> event.getPlayer().getLastPlayed() < plugin.getIWM().getResetEpoch(w))
                .forEach(w -> players.setResets(w, playerUUID, 0));

        // Update the island range of the islands the player owns
        updateIslandRange(user);

        // Set the player's name (it may have changed), but only if it isn't empty
        if (!user.getName().isEmpty()) {
            players.setPlayerName(user);
        } else {
            plugin.logWarning("Player that just logged in has no name! " + playerUUID);
        }

        // Set the primary island to the player's location if this is their island
        plugin.getIslands().getIslandAt(user.getLocation()).filter(i -> user.getUniqueId().equals(i.getOwner()))
                .ifPresent(i -> plugin.getIslands().setPrimaryIsland(playerUUID, i));

        // If mobs have to be removed when a player joins, then wipe all the mobs on his
        // island.
        if (plugin.getIslands().locationIsOnIsland(event.getPlayer(), user.getLocation())
                && Flags.REMOVE_MOBS.isSetForWorld(user.getWorld())) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getIslands().clearArea(user.getLocation()));
        }

        // Clear inventory if required
        clearPlayersInventory(Util.getWorld(event.getPlayer().getWorld()), user);

        // Set island max members and homes based on permissions if this player is the
        // owner of an island
        updateIslandMaxTeamAndHomeSize(user);

        // Add a player to the bStats cache.
        plugin.getMetrics().ifPresent(bStats -> bStats.addPlayer(playerUUID));

        // Create onIsland placeholders
        plugin.getAddonsManager().getGameModeAddons().forEach(addon -> {
            plugin.getPlaceholdersManager()
                    .registerPlaceholder(addon, "onisland_" + user.getName(), asker -> {
                        if (asker == null) {
                            return "";
                        }
                        // Get the user who this applies to
                        User named = User.getInstance(user.getUniqueId());
                        if (named.isOnline()) {
                            return plugin.getIslands().getIslands(addon.getOverWorld(), asker).stream()
                                    .filter(island -> island.onIsland(named.getLocation())).findFirst().map(i -> "true")
                                    .orElse("false");
                        }
                        return "false";
                    });
        });
    }

    private void updateIslandMaxTeamAndHomeSize(User user) {
        plugin.getIWM().getOverWorlds().stream()
                .flatMap(w -> plugin.getIslands().getIslands(w, user.getUniqueId()).stream()) // Flatten the List<Island> into a Stream<Island>
                .filter(Objects::nonNull).filter(i -> user.getUniqueId().equals(i.getOwner())).forEach(i -> {
                    plugin.getIslands().getMaxMembers(i, RanksManager.MEMBER_RANK);
                    plugin.getIslands().getMaxMembers(i, RanksManager.COOP_RANK);
                    plugin.getIslands().getMaxMembers(i, RanksManager.TRUSTED_RANK);
                    plugin.getIslands().getMaxHomes(i);
                });

    }

    private void firstTime(User user) {
        // Make sure the player is loaded into the cache or create the player if they
        // don't exist
        players.getPlayer(user.getUniqueId());

        plugin.getIWM().getOverWorlds().stream().filter(w -> plugin.getIWM().isCreateIslandOnFirstLoginEnabled(w))
                .forEach(w -> {
                    // Even if that'd be extremely unlikely, it's better to check if the player
                    // doesn't have an island already.
                    if (!(plugin.getIslands().hasIsland(w, user)
                            || plugin.getIslands().inTeam(w, user.getUniqueId()))) {
                        int delay = plugin.getIWM().getCreateIslandOnFirstLoginDelay(w);
                        user.sendMessage("commands.island.create.on-first-login", TextVariables.NUMBER,
                                String.valueOf(delay));

                        Runnable createIsland = () -> {
                            // should only execute if:
                            // - abort on logout is false
                            // - abort on logout is true && user is online
                            if (!plugin.getIWM().isCreateIslandOnFirstLoginAbortOnLogout(w) || user.isOnline()) {
                                plugin.getIWM().getAddon(w)
                                        .flatMap(addon -> addon.getPlayerCommand()
                                                .flatMap(command -> command.getSubCommand("create")))
                                        .ifPresent(command -> command.execute(user, "create",
                                                Collections.singletonList(BlueprintsManager.DEFAULT_BUNDLE_NAME)));
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
     * This event will clean players inventory
     * 
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
     * This method clears player inventory and ender chest if given world is
     * quarantined in user data file and it is required by plugin settings.
     * 
     * @param world World where cleaning must occur.
     * @param user  Targeted user.
     */
    private void clearPlayersInventory(@Nullable World world, @NonNull User user) {
        if (user.getUniqueId() == null || world == null)
            return;
        // Clear inventory if required
        Players playerData = players.getPlayer(user.getUniqueId());

        if (playerData != null && playerData.getPendingKicks().contains(world.getName())) {
            if (plugin.getIWM().isOnLeaveResetEnderChest(world)) {
                user.getPlayer().getEnderChest().clear();
            }

            if (plugin.getIWM().isOnLeaveResetInventory(world)) {
                user.getPlayer().getInventory().clear();
            }

            Set<String> kicks = playerData.getPendingKicks();
            kicks.remove(world.getName());
            playerData.setPendingKicks(kicks);

        }
    }

    /**
     * Update island range using player perms
     * @param user user
     */
    private void updateIslandRange(User user) {
        plugin.getIslands().getIslands(user.getUniqueId()).stream()
                .filter(island -> island.getOwner() != null && island.getOwner().equals(user.getUniqueId()))
                .forEach(island -> {
                    // Check if new owner has a different range permission than the island size
                    int range = user.getPermissionValue(plugin.getIWM().getAddon(island.getWorld())
                            .map(GameModeAddon::getPermissionPrefix).orElse("") + "island.range",
                            island.getRawProtectionRange());
                    // Range cannot be greater than the island distance
                    range = Math.min(range, plugin.getIWM().getIslandDistance(island.getWorld()));
                    // Range can go up or down
                    if (range != island.getRawProtectionRange()) {
                        user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER,
                                String.valueOf(range));
                        int oldRange = island.getProtectionRange();
                        island.setProtectionRange(range);

                        plugin.log("Island protection range changed from " + oldRange + " to "
                                + island.getProtectionRange() + " for " + user.getName() + " due to permission.");
                        // Call Protection Range Change event. Does not support canceling.
                        IslandEvent.builder().island(island).location(island.getProtectionCenter())
                                .reason(IslandEvent.Reason.RANGE_CHANGE).involvedPlayer(user.getUniqueId()).admin(true)
                                .protectionRange(island.getProtectionRange(), oldRange).build();
                    }
                });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        // Remove any coops if all the island players have left
        // Go through all the islands this player is a member of, check if all members
        // have left, remove coops
        plugin.getIslands().getIslands(event.getPlayer().getUniqueId()).stream()
                .filter(island -> island.getMembers().containsKey(event.getPlayer().getUniqueId())).forEach(island -> {
                    // Are there any online players still for this island?
                    if (Bukkit.getOnlinePlayers().stream().filter(p -> !event.getPlayer().equals(p))
                            .noneMatch(p -> island.inTeam(p.getUniqueId()))) {
                        // No, there are no more players online on this island
                        // Tell players they are being removed
                        island.getMembers().entrySet().stream().filter(e -> e.getValue() == RanksManager.COOP_RANK)
                                .forEach(e -> User.getInstance(e.getKey()).sendMessage(
                                        "commands.island.team.uncoop.all-members-logged-off", TextVariables.NAME,
                                        plugin.getPlayers().getName(island.getOwner())));
                        // Remove any coop players on this island
                        island.removeRank(RanksManager.COOP_RANK);
                    }
                });
        // Remove any coop associations from the player logging out
        plugin.getIslands().clearRank(RanksManager.COOP_RANK, event.getPlayer().getUniqueId());
        // Remove any onisland placeholder
        plugin.getAddonsManager().getGameModeAddons().forEach(addon -> plugin.getPlaceholdersManager()
                .unregisterPlaceholder(addon, "onisland_" + event.getPlayer().getName()));
        User.removePlayer(event.getPlayer());
    }
}
