package us.tastybento.bskyblock.commands.island.teams;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.scheduler.BukkitRunnable;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class IslandTeamKickCommand extends AbstractIslandTeamCommand {

    Set<UUID> kickSet;

    public IslandTeamKickCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "kick");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setParameters("commands.island.team.kick.parameters");
        setDescription("commands.island.team.kick.description");
        kickSet = new HashSet<>();
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!getIslands().inTeam(user.getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (!getTeamLeader(user.getWorld(), user).equals(user.getUniqueId())) {
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
        if (!getIslands().getMembers(user.getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (!getSettings().isKickConfirmation() || kickSet.contains(targetUUID)) {
            kickSet.remove(targetUUID);
            User.getInstance(targetUUID).sendMessage("commands.island.team.kick.leader-kicked");
            getIslands().removePlayer(user.getWorld(), targetUUID);
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