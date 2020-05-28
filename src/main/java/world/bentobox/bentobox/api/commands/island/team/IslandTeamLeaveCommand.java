package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamLeaveCommand extends ConfirmableCommand {

    public IslandTeamLeaveCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }

    @Override
    public void setup() {
        setPermission("island.team.leave");
        setOnlyPlayer(true);
        setDescription("commands.island.team.leave.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("commands.island.team.leave.cannot-leave");
            return false;
        }
        if (!getSettings().isLeaveConfirmation()) {
            leave(user);
            return true;
        } else {
            // Check resets
            if (getIWM().isLeaversLoseReset(getWorld())) {
                showResets(user);
            }
            this.askConfirmation(user, () -> leave(user));
            return false;
        }
    }

    private void showResets(User user) {
        int resetsLeft = getPlayers().getResetsLeft(getWorld(), user.getUniqueId());
        if (resetsLeft != -1) {
            // Resets are not unlimited here
            if (resetsLeft == 0) {
                // No resets allowed
                user.sendMessage("commands.island.reset.none-left");
            } else {
                // Still some resets left
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(resetsLeft));
            }
        }

    }

    private void leave(User user) {
        Island island = getIslands().getIsland(getWorld(), user);
        UUID ownerUUID = getIslands().getOwner(getWorld(), user.getUniqueId());
        if (ownerUUID != null) {
            User.getInstance(ownerUUID).sendMessage("commands.island.team.leave.left-your-island", TextVariables.NAME, user.getName());
        }
        getIslands().setLeaveTeam(getWorld(), user.getUniqueId());
        // Execute commands when leaving
        Util.runCommands(user, getIWM().getOnLeaveCommands(island.getWorld()), "leave");
        // Remove money inventory etc.
        if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }
        // Reset the health
        if (getIWM().isOnLeaveResetHealth(getWorld())) {
            user.getPlayer().setHealth(20.0D);
        }

        // Reset the hunger
        if (getIWM().isOnLeaveResetHunger(getWorld())) {
            user.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (getIWM().isOnLeaveResetXP(getWorld())) {
            user.getPlayer().setTotalExperience(0);
        }
        // Add cooldown for this player and target
        if (getSettings().getInviteCooldown() > 0 && getParent() != null) {
            // Get the invite class from the parent
            getParent().getSubCommand("invite").ifPresent(c -> c.setCooldown(island.getUniqueId(), user.getUniqueId().toString(), getSettings().getInviteCooldown() * 60));
        }
        // Remove reset if required
        if (getIWM().isLeaversLoseReset(getWorld())) {
            // Add a reset
            getPlayers().addReset(getWorld(), user.getUniqueId());
            // Notify how many resets are left
            showResets(user);
        }
        user.sendMessage("commands.island.team.leave.success");
        // Fire event
        TeamEvent.builder()
        .island(island)
        .reason(TeamEvent.Reason.LEAVE)
        .involvedPlayer(user.getUniqueId())
        .build();
        IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(user), RanksManager.VISITOR_RANK)
                .build();
    }
}