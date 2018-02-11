package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamSetownerCommand extends AbstractIslandTeamCommand {

    public IslandTeamSetownerCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "setleader");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setParameters("commands.island.team.setowner.parameters");
        setDescription("commands.island.team.setowner.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Can use if in a team
        boolean inTeam = getPlugin().getPlayers().inTeam(playerUUID);
        UUID teamLeaderUUID = getPlugin().getIslands().getTeamLeader(playerUUID);
        if (!(inTeam && teamLeaderUUID.equals(playerUUID))) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        //getPlugin().getLogger().info("DEBUG: arg[0] = " + args.get(0));
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getPlayers().inTeam(playerUUID)) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (!teamLeaderUUID.equals(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (targetUUID.equals(playerUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
            return false;
        }
        if (!getPlugin().getIslands().getMembers(playerUUID).contains(targetUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.target-is-not-member");
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                        .getIsland(playerUUID))
                .reason(TeamEvent.Reason.MAKELEADER)
                .involvedPlayer(targetUUID)
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        // target is the new leader
        getIslands().getIsland(playerUUID).setOwner(targetUUID);
        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]", getPlayers().getName(targetUUID));

        // Check if online
        User target = User.getInstance(targetUUID);
        target.sendMessage("commands.island.team.setowner.you-are-the-owner");
        if (target.isOnline()) {
            // Check if new leader has a lower range permission than the island size
            boolean hasARangePerm = false;
            int range = getSettings().getIslandProtectionRange();
            // Check for zero protection range
            Island islandByOwner = getIslands().getIsland(targetUUID);
            if (islandByOwner.getProtectionRange() == 0) {
                getPlugin().getLogger().warning("Player " + user.getName() + "'s island had a protection range of 0. Setting to default " + range);
                islandByOwner.setProtectionRange(range);
            }
            for (PermissionAttachmentInfo perms : target.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Constants.PERMPREFIX + "island.range.")) {
                    if (perms.getPermission().contains(Constants.PERMPREFIX + "island.range.*")) {
                        // Ignore
                        break;
                    } else {
                        String[] spl = perms.getPermission().split(Constants.PERMPREFIX + "island.range.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                getPlugin().getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");

                            } else {
                                hasARangePerm = true;
                                range = Math.max(range, Integer.valueOf(spl[1]));
                            }
                        }
                    }
                }
            }
            // Only set the island range if the player has a perm to override the default
            if (hasARangePerm) {
                // Do some sanity checking
                if (range % 2 != 0) {
                    range--;
                }
                // Get island range

                // Range can go up or down
                if (range != islandByOwner.getProtectionRange()) {
                    user.sendMessage("commands.admin.setrange.range-updated", "[number]", String.valueOf(range));
                    target.sendMessage("commands.admin.setrange.range-updated", "[number]", String.valueOf(range));
                    getPlugin().getLogger().info(
                            "Makeleader: Island protection range changed from " + islandByOwner.getProtectionRange() + " to "
                                    + range + " for " + user.getName() + " due to permission.");
                }
                islandByOwner.setProtectionRange(range);
            }
        }
        getIslands().save(true);
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = (!args.isEmpty() ? args.getLast() : "");
        for (UUID member : getPlugin().getIslands().getMembers(user.getUniqueId())) {
            options.add(getPlugin().getServer().getOfflinePlayer(member).getName());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}