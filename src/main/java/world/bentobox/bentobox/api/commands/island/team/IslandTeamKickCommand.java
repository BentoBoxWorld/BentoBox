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


public class IslandTeamKickCommand extends ConfirmableCommand {

    public IslandTeamKickCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "kick");
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.kick.parameters");
        setDescription("commands.island.team.kick.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (!user.getUniqueId().equals(getOwner(getWorld(), user))) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (targetUUID.equals(user.getUniqueId())) {
            user.sendMessage("commands.island.kick.cannot-kick");
            return false;
        }
        if (!getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (!getSettings().isKickConfirmation()) {
            kick(user, targetUUID);
            return true;
        } else {
            askConfirmation(user, () -> kick(user, targetUUID));
            return false;
        }
    }

    private void kick(User user, UUID targetUUID) {
        User target = User.getInstance(targetUUID);
        target.sendMessage("commands.island.team.kick.owner-kicked");
        Island oldIsland = getIslands().getIsland(getWorld(), targetUUID);
        getIslands().removePlayer(getWorld(), targetUUID);
        // Remove money inventory etc.
        if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
            if (target.isOnline()) {
                target.getPlayer().getEnderChest().clear();
            }
            else {
                getPlayers().getPlayer(targetUUID).addToPendingKick(getWorld());
                getPlayers().save(targetUUID);
            }
        }
        if (getIWM().isOnLeaveResetInventory(getWorld())) {
            if (target.isOnline()) {
                target.getPlayer().getInventory().clear();
            }
            else {
                getPlayers().getPlayer(targetUUID).addToPendingKick(getWorld());
                getPlayers().save(targetUUID);
            }
        }
        if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(target, vault.getBalance(target)));
        }
        user.sendMessage("general.success");
        // Fire event
        IslandBaseEvent e = TeamEvent.builder()
                .island(oldIsland)
                .reason(TeamEvent.Reason.KICK)
                .involvedPlayer(targetUUID)
                .build();
        Bukkit.getServer().getPluginManager().callEvent(e);

        // Add cooldown for this player and target
        if (getSettings().getInviteCooldown() > 0 && getParent() != null) {
            // Get the invite class from the parent
            getParent().getSubCommand("invite").ifPresent(c -> c.setCooldown(user.getUniqueId(), targetUUID, getSettings().getInviteCooldown() * 60));
        }
    }
}
