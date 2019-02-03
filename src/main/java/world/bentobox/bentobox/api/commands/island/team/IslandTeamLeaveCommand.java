package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class IslandTeamLeaveCommand extends ConfirmableCommand {

    public IslandTeamLeaveCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }

    @Override
    public void setup() {
        setPermission("island.team");
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
            this.askConfirmation(user, () -> leave(user));
            return false;
        }
    }

    private void leave(User user) {
        Island island = getIslands().getIsland(getWorld(), user);
        UUID ownerUUID = getIslands().getOwner(getWorld(), user.getUniqueId());
        if (ownerUUID != null) {
            User.getInstance(ownerUUID).sendMessage("commands.island.team.leave.left-your-island", TextVariables.NAME, user.getName());
        }
        getIslands().setLeaveTeam(getWorld(), user.getUniqueId());
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
        user.sendMessage("general.success");
        // Fire event
        IslandBaseEvent e = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.LEAVE)
                .involvedPlayer(user.getUniqueId())
                .build();
        Bukkit.getServer().getPluginManager().callEvent(e);
    }

}