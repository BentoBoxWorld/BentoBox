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

public class AdminDeleteCommand extends ConfirmableCommand {

    private @Nullable UUID targetUUID;
    private Island island;

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
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        // Convert name to a UUID
        targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Check island exists
        if (!getIslands().hasIsland(getWorld(), targetUUID) && !getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        if (args.size() == 1) {
            // Check if player is owner of any islands
            if (getIslands().getIslands(getWorld(), targetUUID).stream().filter(Island::hasTeam)
                    .anyMatch(is -> targetUUID.equals(is.getOwner()))) {
                user.sendMessage("commands.admin.delete.cannot-delete-owner");
                return false;
            }
            // This is a delete everything request
            return true;
        }

        // Get the island
        User target = User.getInstance(targetUUID);
        // They named the island to go to
        Map<String, IslandInfo> names = IslandGoCommand.getNameIslandMap(target, getWorld());
        final String name = String.join(" ", args.subList(1, args.size()));
        if (!names.containsKey(name)) {
            // Failed home name check
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            names.keySet()
                    .forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
            return false;
        } else {
            IslandInfo info = names.get(name);
            island = info.island();
        }

        // Team members should be kicked before deleting otherwise the whole team will become weird
        if (island.hasTeam() && targetUUID.equals(island.getOwner())) {
            user.sendMessage("commands.admin.delete.cannot-delete-owner");
            return false;
        }
        if (names.size() == 1) {
            // This is the only island they have so, no need to specify it
            island = null;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Confirm
        if (island == null) {
            // Delete the player entirely
            askConfirmation(user, () -> deletePlayer(user));
        } else {
            // Just delete the player's island
            askConfirmation(user, () -> deleteIsland(user, island));
        }
        return true;
    }

    private void deleteIsland(User user, Island oldIsland) {
        // Fire island preclear event
        IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.PRECLEAR).island(oldIsland)
                .oldIsland(oldIsland).location(oldIsland.getCenter()).build();
        user.sendMessage("commands.admin.delete.deleted-island", TextVariables.XYZ,
                Util.xyz(oldIsland.getCenter().toVector()));
        getIslands().deleteIsland(oldIsland, true, targetUUID);

    }

    private void deletePlayer(User user) {
        // Delete player and island
        for (Island oldIsland : getIslands().getIslands(getWorld(), targetUUID)) {
            deleteIsland(user, oldIsland);
        }
        // Check if player is online and on the island
        User target = User.getInstance(targetUUID);
        // Remove target from any and all islands in the world
        getIslands().removePlayer(getWorld(), targetUUID);
        if (target.isPlayer() && target.isOnline()) {
            cleanUp(target);
        }
        user.sendMessage("general.success");
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
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
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
