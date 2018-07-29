package world.bentobox.bbox.commands.island.team;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;

public class IslandTeamKickCommand extends CompositeCommand {

    Set<UUID> kickSet;

    public IslandTeamKickCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "kick");
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setParameters("commands.island.team.kick.parameters");
        setDescription("commands.island.team.kick.description");
        kickSet = new HashSet<>();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (!getTeamLeader(getWorld(), user).equals(user.getUniqueId())) {
            user.sendMessage("general.errors.not-leader");
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
            user.sendMessage("general.errors.unknown-player");
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
        if (!getSettings().isKickConfirmation() || kickSet.contains(targetUUID)) {
            kickSet.remove(targetUUID);
            User.getInstance(targetUUID).sendMessage("commands.island.team.kick.leader-kicked");
            getIslands().removePlayer(getWorld(), targetUUID);
            // Remove money inventory etc.
            if (getIWM().isOnLeaveResetEnderChest(getWorld())) {
                user.getPlayer().getEnderChest().clear();
            }
            if (getIWM().isOnLeaveResetInventory(getWorld())) {
                user.getPlayer().getInventory().clear();
            }
            if (getSettings().isUseEconomy() && getIWM().isOnLeaveResetMoney(getWorld())) {
                // TODO: needs Vault
            }
            user.sendMessage("general.success");
            return true;
        } else {
            user.sendMessage("commands.island.team.kick.type-again");
            kickSet.add(targetUUID);
            new BukkitRunnable() {

                @Override
                public void run() {
                    if (kickSet.contains(targetUUID)) {
                        kickSet.remove(targetUUID);
                        user.sendMessage("general.errors.command-cancelled");
                    }
                }}.runTaskLater(getPlugin(), getSettings().getKickWait() * 20);
            return false;
        }
    }


}