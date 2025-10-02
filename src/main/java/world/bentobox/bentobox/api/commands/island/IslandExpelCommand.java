package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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
 * Handles the island expel command (/island expel).
 * <p>
 * This command allows island owners and team members to remove visitors from their island.
 * Unlike banning, expelling is temporary - players can return later.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement</li>
 *   <li>Protection for team members</li>
 *   <li>Protection for admins and moderators</li>
 *   <li>Smart teleport handling (home island, spawn, or new island)</li>
 *   <li>Sound effects</li>
 *   <li>Event system integration</li>
 * </ul>
 * <p>
 * Permission nodes:
 * <ul>
 *   <li>{@code island.expel} - Base permission</li>
 *   <li>{@code [gamemode].admin.noexpel} - Cannot be expelled</li>
 *   <li>{@code [gamemode].mod.bypassexpel} - Cannot be expelled</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.4.0
 */
public class IslandExpelCommand extends CompositeCommand {

    /** Common message keys to avoid duplication */
    private static final String CANNOT_EXPEL = "commands.island.expel.cannot-expel";
    private static final String SUCCESS = "commands.island.expel.success";

    /** Cached target player, set during canExecute and used in execute */
    private @Nullable User target;

    public IslandExpelCommand(CompositeCommand islandCommand) {
        super(islandCommand, "expel");
    }

    @Override
    public void setup() {
        setOnlyPlayer(true);
        setPermission("island.expel");
        setParametersHelp("commands.island.expel.parameters");
        setDescription("commands.island.expel.description");
        setConfigurableRankCommand();
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Correct number of arguments</li>
     *   <li>Player has an island or is in a team</li>
     *   <li>Player has sufficient rank</li>
     *   <li>Target player exists and is online</li>
     *   <li>Target is not self, team member, or protected</li>
     *   <li>Target is actually on the island</li>
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
        if (!getIslands().inTeam(getWorld(), user.getUniqueId()) && !getIslands().hasIsland(getWorld(), user)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        Island island = getIslands().getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);
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
        // Player cannot expel themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.expel.cannot-expel-yourself");
            return false;
        }
        // Or team member
        if (island.inTeam(targetUUID)) {
            user.sendMessage("commands.island.expel.cannot-expel-member");
            return false;
        }
        // Cannot expel offline players or invisible players
        target = User.getInstance(targetUUID);
        if (!target.isOnline() || !user.getPlayer().canSee(Bukkit.getPlayer(targetUUID))) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        // Not on island
        if (!getIslands().locationIsOnIsland(user.getPlayer(), target.getLocation())) {
            user.sendMessage("commands.island.expel.not-on-island");
            return false;
        }
        // Cannot ban ops
        if (target.isOp() || target.hasPermission(this.getPermissionPrefix() + "admin.noexpel")
                || target.hasPermission(this.getPermissionPrefix() + "mod.bypassexpel")) {
            user.sendMessage(CANNOT_EXPEL);
            return false;
        }
        return true;
    }

    /**
     * Handles the expulsion process.
     * <p>
     * Flow:
     * <ul>
     *   <li>Fires cancellable expel event</li>
     *   <li>Notifies target</li>
     *   <li>Plays explosion sound</li>
     *   <li>Teleports target to:
     *     <ul>
     *       <li>Their home island</li>
     *       <li>Spawn (if exists)</li>
     *       <li>New island (if possible)</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Finished error checking - expel player
        Island island = getIslands().getIsland(getWorld(), user);
        // Fire event
        IslandBaseEvent expelEvent = IslandEvent.builder().island(island).involvedPlayer(target.getUniqueId())
                .admin(false).reason(IslandEvent.Reason.EXPEL).build();
        if (expelEvent.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(expelEvent.isCancelled())) {
            user.sendMessage(CANNOT_EXPEL);
            return false;
        }
        target.sendMessage("commands.island.expel.player-expelled-you", TextVariables.NAME, user.getName(),
                TextVariables.DISPLAY_NAME, user.getDisplayName());
        island.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
        if (getIslands().hasIsland(getWorld(), target) || getIslands().inTeam(getWorld(), target.getUniqueId())) {
            // Success
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME,
                    target.getDisplayName());
            // Teleport home
            getIslands().homeTeleportAsync(getWorld(), target.getPlayer());
            return true;
        } else if (getIslands().getSpawn(getWorld()).isPresent()) {
            // Success
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME,
                    target.getDisplayName());
            getIslands().spawnTeleport(getWorld(), target.getPlayer());
            return true;
        } else if (getIWM().getAddon(getWorld())
                .map(gm -> gm.getPlayerCommand().map(pc -> pc.getSubCommand("create").isPresent()).orElse(false))
                .orElse(false) && target.performCommand(this.getTopLabel() + " create")) {
            getAddon().logWarning("Expel: " + target.getName() + " had no island, so one was created");
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName(), TextVariables.DISPLAY_NAME,
                    target.getDisplayName());
            return true;
        }

        getAddon().logError("Expel: " + target.getName() + " had no island, and one could not be created");
        user.sendMessage(CANNOT_EXPEL);
        return false;
    }

    /**
     * Provides tab completion for online players currently on the island.
     * Excludes:
     * <ul>
     *   <li>Command issuer</li>
     *   <li>Invisible players</li>
     *   <li>Server operators</li>
     *   <li>Protected players (admin.noexpel, mod.bypassexpel)</li>
     * </ul>
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user);
        if (island != null) {
            List<String> options = island.getPlayersOnIsland().stream().filter(p -> !p.equals(user.getPlayer())) // Not
                    // self
                    .filter(p -> user.getPlayer().canSee(p)) // Not invisible
                    .filter(p -> !p.isOp()) // Not op
                    .filter(p -> !p.hasPermission(this.getPermissionPrefix() + "admin.noexpel"))
                    .filter(p -> !p.hasPermission(this.getPermissionPrefix() + "mod.bypassexpel")).map(Player::getName)
                    .toList();

            String lastArg = !args.isEmpty() ? args.getLast() : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
