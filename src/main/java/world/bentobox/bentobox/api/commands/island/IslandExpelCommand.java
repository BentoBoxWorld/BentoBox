package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.4.0
 */
public class IslandExpelCommand extends CompositeCommand {

    private static final String CANNOT_EXPEL = "commands.island.expel.cannot-expel";
    private static final String SUCCESS = "commands.island.expel.success";

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
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Player cannot expel themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.expel.cannot-expel-yourself");
            return false;
        }
        // Or team member
        if (getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.expel.cannot-expel-member");
            return false;
        }
        // Cannot expel offline players
        target = User.getInstance(targetUUID);
        if (!target.isOnline()) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        // Not on island
        if (!getIslands().locationIsOnIsland(user.getPlayer(), target.getLocation())) {
            user.sendMessage("commands.island.expel.not-on-island");
            return false;
        }
        // Cannot ban ops
        if (target.isOp() ||
                target.hasPermission(this.getPermissionPrefix() + "admin.noexpel") ||
                target.hasPermission(this.getPermissionPrefix() + "mod.bypassexpel")) {
            user.sendMessage(CANNOT_EXPEL);
            return false;
        }
        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Finished error checking - expel player
        Island island = getIslands().getIsland(getWorld(), user);
        // Fire event
        IslandBaseEvent expelEvent = IslandEvent.builder()
                .island(island)
                .involvedPlayer(target.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.EXPEL)
                .build();
        if (expelEvent.isCancelled()) {
            user.sendMessage(CANNOT_EXPEL);
            return false;
        }

        target.sendMessage("commands.island.expel.player-expelled-you", TextVariables.NAME, user.getName());
        island.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
        if (getIslands().hasIsland(getWorld(), target)) {
            // Success
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName());
            // Teleport home
            getIslands().homeTeleportAsync(getWorld(), target.getPlayer());
            return true;
        } else if (getIslands().getSpawn(getWorld()).isPresent()){
            // Success
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName());
            getIslands().spawnTeleport(getWorld(), target.getPlayer());
            return true;
        } else if (getIWM().getAddon(getWorld())
                .map(gm -> gm.getPlayerCommand()
                        .map(pc -> pc.getSubCommand("create").isPresent())
                        .orElse(false))
                .orElse(false)
                && target.performCommand(this.getTopLabel() + " create")) {
            getAddon().logWarning("Expel: " + target.getName() + " had no island, so one was created");
            user.sendMessage(SUCCESS, TextVariables.NAME, target.getName());
            return true;
        }

        getAddon().logError("Expel: " + target.getName() + " had no island, and one could not be created");
        user.sendMessage(CANNOT_EXPEL);
        return false;
    }
    
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getPlayersOnIsland().stream()
                    .filter(p -> !p.equals(user.getPlayer()))
                    .map(Player::getName).collect(Collectors.toList());

            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
