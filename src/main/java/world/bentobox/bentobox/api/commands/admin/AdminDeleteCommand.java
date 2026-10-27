package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;

public class AdminDeleteCommand extends ConfirmableCommand {

    private static final String CANNOT_DELETE_OWNER = "commands.admin.delete.cannot-delete-owner";

    private record DeleteTarget(Island island, UUID player) { }

    public AdminDeleteCommand(CompositeCommand parent) {
        super(parent, "delete");
    }

    @Override
    public void setup() {
        setPermission("admin.delete");
        setParametersHelp("commands.admin.delete.parameters");
        setDescription("commands.admin.delete.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return resolveTarget(user, args) != null;
    }

    private @Nullable DeleteTarget resolveTarget(User user, List<String> args) {
        if (args.isEmpty()) {
            // No player named - delete the island the admin is standing on
            return resolveStandingOn(user);
        }
        // Convert name to a UUID
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        // Check island exists
        if (!getIslands().hasIsland(getWorld(), targetUUID) && !getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }

        if (args.size() == 1) {
            // Check if player is owner of any islands
            if (getIslands().getIslands(getWorld(), targetUUID).stream().filter(Island::hasTeam)
                    .anyMatch(is -> targetUUID.equals(is.getOwner()))) {
                user.sendMessage(CANNOT_DELETE_OWNER);
                return null;
            }
            // This is a delete everything request
            return new DeleteTarget(null, targetUUID);
        }

        // Get the island
        User target = User.getInstance(targetUUID);
        // They named the island to go to
        Map<String, IslandInfo> names = IslandGoCommand.getNameIslandMap(target, getWorld());
        final String name = String.join(" ", args.subList(1, args.size()));
        Island island;
        if (!names.containsKey(name)) {
            // Failed home name check
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            names.keySet()
                    .forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
            return null;
        } else {
            IslandInfo info = names.get(name);
            island = info.island();
        }

        // Team members should be kicked before deleting otherwise the whole team will become weird.
        // This also covers a member naming the team's island: deleting it would remove the owner's.
        if (island.hasTeam()) {
            user.sendMessage(CANNOT_DELETE_OWNER);
            return null;
        }
        if (names.size() == 1) {
            // This is the only island they have so, no need to specify it
            island = null;
        }
        return new DeleteTarget(island, targetUUID);
    }

    /**
     * Handles the no-argument form: delete the island the admin is standing on.
     * Returns a per-request target for deferred confirmation.
     *
     * @param user the admin running the command
     * @return deletion target, or null if there is no deletable island here
     */
    private @Nullable DeleteTarget resolveStandingOn(User user) {
        // Location-based deletion only makes sense for an in-game player
        if (!user.isPlayer()) {
            this.showHelp(this, user);
            return null;
        }
        if (!getWorld().equals(user.getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return null;
        }
        Optional<Island> opIsland = getIslands().getIslandAt(user.getLocation());
        if (opIsland.isEmpty()) {
            user.sendMessage("general.errors.not-on-island");
            return null;
        }
        Island island = opIsland.get();
        // Do not orphan a team - its members have to be kicked first
        if (island.hasTeam()) {
            user.sendMessage(CANNOT_DELETE_OWNER);
            return null;
        }
        // The involved player for the deletion event is the island owner, if any
        return new DeleteTarget(island, island.getOwner());
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        DeleteTarget resolved = resolveTarget(user, args);
        if (resolved == null) return false;
        final Island targetIsland = resolved.island();
        final UUID target = resolved.player();
        // Confirm
        if (targetIsland == null) {
            // Delete the player entirely
            askConfirmation(user, () -> deletePlayer(user, target));
        } else {
            // Just delete the player's island
            askConfirmation(user, () -> deleteIsland(user, targetIsland, target));
        }
        return true;
    }

    private boolean deleteIsland(User user, Island oldIsland, UUID targetUUID) {
        // Fire island preclear event
        IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.PRECLEAR).island(oldIsland)
                .oldIsland(oldIsland).location(oldIsland.getCenter()).build();

        // Soft-delete: mark deletable, clear owner, save. The region files
        // and DB row are reaped later by PurgeRegionsService /
        // HousekeepingManager so the row stays visible until the chunks
        // are actually gone from disk.
        if (!getIslands().deleteIsland(oldIsland, true, targetUUID)) {
            // A listener vetoed the delete: do not claim it happened.
            user.sendMessage("general.errors.command-cancelled");
            return false;
        }
        user.sendMessage("commands.admin.delete.deleted-island", TextVariables.XYZ,
                Util.xyz(oldIsland.getCenter().toVector()));
        return true;
    }

    private void deletePlayer(User user, UUID targetUUID) {
        // Delete the player's own island only. The rest of the list is islands the player is
        // just a member of, which belong to somebody else - removePlayer below drops them.
        boolean vetoed = false;
        for (Island oldIsland : getIslands().getIslands(getWorld(), targetUUID)) {
            if (targetUUID.equals(oldIsland.getOwner()) && !deleteIsland(user, oldIsland, targetUUID)) {
                vetoed = true;
            }
        }
        // Check if player is online and on the island
        assert targetUUID != null;
        User target = User.getInstance(targetUUID);
        // Remove target from any and all islands in the world
        getIslands().removePlayer(getWorld(), targetUUID);
        if (target.isPlayer() && target.isOnline()) {
            if (TaskScheduler.isFolia()) {
                TaskScheduler.userFuture(getPlugin(), target, () -> cleanUp(target)).exceptionally(error -> {
                    getPlugin().logStacktrace(error);
                    return null;
                });
            } else cleanUp(target);
        }
        user.sendMessage(vetoed ? "general.errors.command-cancelled" : "general.success");
    }

    private void cleanUp(User target) {
        // Remove money inventory etc.
        if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
            target.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld())) {
            target.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(target, vault.getBalance(target)));
        }
        // Reset the health
        if (getIWM().isOnLeaveResetHealth(getWorld())) {
            Util.resetHealth(target.getPlayer());
        }

        // Reset the hunger
        if (getIWM().isOnLeaveResetHunger(getWorld())) {
            target.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (getIWM().isOnLeaveResetXP(getWorld())) {
            // Player collected XP (displayed)
            target.getPlayer().setLevel(0);
            target.getPlayer().setExp(0);
            // Player total XP (not displayed)
            target.getPlayer().setTotalExperience(0);
        }

        // Execute commands when leaving
        Util.runCommands(target, target.getName(), getIWM().getOnLeaveCommands(getWorld()), "leave");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        if (args.size() == 2) {
            return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), lastArg));
        }
        if (args.size() == 3) {
            UUID target = Util.getUUID(args.get(1));
            return target == null ? Optional.empty()
                    : Optional.of(Util.tabLimit(
                            new ArrayList<>(
                                    IslandGoCommand.getNameIslandMap(User.getInstance(target), getWorld()).keySet()),
                            lastArg));
        }
        return Optional.empty();
    }

}
