package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island ban command, which allows island owners and team members
 * to ban players from their island.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement</li>
 *   <li>Ban limits based on permissions</li>
 *   <li>Cooldown system</li>
 *   <li>Protection against banning team members</li>
 *   <li>Admin protection (cannot ban ops or admins)</li>
 *   <li>Automatic teleport of banned online players</li>
 * </ul>
 * <p>
 * Permission: {@code island.ban}
 * <br>
 * Sub-permissions:
 * <ul>
 *   <li>{@code island.ban.maxlimit.[number]} - Sets maximum number of bans</li>
 *   <li>{@code admin.noban} - Prevents player from being banned</li>
 * </ul>
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandBanCommand extends CompositeCommand {

    /**
     * Cached target player to ban, set during canExecute and used in execute.
     */
    private @Nullable User target;

    public IslandBanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "ban");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.ban.parameters");
        setDescription("commands.island.ban.description");
        setConfigurableRankCommand();
    }

    /**
     * Checks if the command can be executed by this user.
     * <p>
     * Validation checks:
     * <ul>
     *   <li>Correct number of arguments</li>
     *   <li>Player has an island or is in a team</li>
     *   <li>Player has sufficient rank</li>
     *   <li>Target player exists</li>
     *   <li>Player isn't trying to ban themselves</li>
     *   <li>Target isn't a team member</li>
     *   <li>Target isn't already banned</li>
     *   <li>Ban cooldown has expired</li>
     *   <li>Target isn't an op or admin</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island or be in a team
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())
                && !getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        Island island = Objects.requireNonNull(getIslands().getIsland(getWorld(), user));
        int rank = island.getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Player cannot ban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-yourself");
            return false;
        }
        if (getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()).inTeam(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-member");
            return false;
        }
        if (island.isBanned(targetUUID)) {
            user.sendMessage("commands.island.ban.player-already-banned");
            return false;
        }
        if (getSettings().getBanCooldown() > 0 && checkCooldown(user, island.getUniqueId(), targetUUID.toString())) {
            return false;
        }
        target = User.getInstance(targetUUID);
        // Cannot ban ops
        if (target.isOp() || (target.isOnline() && target.hasPermission(this.getPermissionPrefix() + "admin.noban"))) {
            user.sendMessage("commands.island.ban.cannot-ban");
            return false;
        }
        return true;
    }

    /**
     * Executes the ban after all checks have passed.
     * Delegates to the {@link #ban(User, User)} method.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Finished error checking - start the banning
        return ban(user, target);
    }

    /**
     * Performs the actual ban operation.
     * <p>
     * Process:
     * <ul>
     *   <li>Checks ban limit</li>
     *   <li>Fires ban event (cancellable)</li>
     *   <li>Applies the ban</li>
     *   <li>Notifies both parties</li>
     *   <li>Teleports banned player if online and on island</li>
     * </ul>
     *
     * @param issuer The player issuing the ban
     * @param target The player being banned
     * @return true if the ban was successful, false otherwise
     */
    private boolean ban(@NonNull User issuer, User target) {
        Island island = Objects.requireNonNull(getIslands().getIsland(getWorld(), issuer.getUniqueId()));

        // Check if player can ban any more players
        int banLimit = issuer.getPermissionValue(getPermissionPrefix() + "ban.maxlimit",
                getIWM().getBanLimit(getWorld()));
        if (banLimit <= -1 || island.getBanned().size() < banLimit) {
            // Run the event
            IslandBaseEvent banEvent = IslandEvent.builder().island(island).involvedPlayer(target.getUniqueId())
                    .admin(false).reason(IslandEvent.Reason.BAN).build();
            if (banEvent.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(banEvent.isCancelled())) {
                // Banning was blocked due to an event cancellation. Fail silently.
                return false;
            }
            // Event is not cancelled
            if (island.ban(issuer.getUniqueId(), target.getUniqueId())) {
                issuer.sendMessage("commands.island.ban.player-banned", TextVariables.NAME, target.getName(),
                        TextVariables.DISPLAY_NAME, target.getDisplayName());
                target.sendMessage("commands.island.ban.owner-banned-you", TextVariables.NAME, issuer.getName(),
                        TextVariables.DISPLAY_NAME, issuer.getDisplayName());
                // If the player is online, has an island and on the banned island, move them
                // home immediately
                if (target.isOnline() && getIslands().hasIsland(getWorld(), target.getUniqueId())
                        && island.onIsland(target.getLocation())) {
                    getIslands().homeTeleportAsync(getWorld(), target.getPlayer());
                    island.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
                }
                return true;
            }
        } else {
            issuer.sendMessage("commands.island.ban.cannot-ban-more-players");
        }
        // Fail silently.
        return false;
    }

    /**
     * Provides tab completion for player names.
     * <p>
     * Only shows:
     * <ul>
     *   <li>Online players</li>
     *   <li>Players not already banned</li>
     *   <li>Players the command issuer can see</li>
     *   <li>Players whose names match the partial input</li>
     * </ul>
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(user.getUniqueId()))
                    .filter(p -> !island.isBanned(p.getUniqueId())).filter(p -> user.getPlayer().canSee(p))
                    .map(Player::getName).toList();
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
