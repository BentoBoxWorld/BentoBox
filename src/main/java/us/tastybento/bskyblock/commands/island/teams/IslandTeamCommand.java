package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.permissions.PermissionAttachmentInfo;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamCommand extends AbstractIslandTeamCommand {

    private static final boolean DEBUG = false;

    public IslandTeamCommand(CompositeCommand islandCommand) {
        super(islandCommand, "team");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setUsage("commands.island.team.usage");
    }

    @Override
    public void setup() {
        new IslandTeamInviteCommand(this);
        new IslandTeamLeaveCommand(this);
        new IslandTeamPromoteCommand(this);
        new IslandTeamSetownerCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: executing team command for " + playerUUID);
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                .getIsland(playerUUID))
                .reason(TeamReason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        UUID teamLeaderUUID = getTeamLeader(user);
        Set<UUID> teamMembers = getMembers(user);
        if (teamLeaderUUID.equals(playerUUID)) {
            int maxSize = Settings.maxTeamSize;
            for (PermissionAttachmentInfo perms : user.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Settings.PERMPREFIX + "team.maxsize.")) {
                    if (perms.getPermission().contains(Settings.PERMPREFIX + "team.maxsize.*")) {
                        maxSize = Settings.maxTeamSize;
                        break;
                    } else {
                        // Get the max value should there be more than one
                        String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "team.maxsize.");
                        if (spl.length > 1) {
                            if (!NumberUtils.isDigits(spl[1])) {
                                plugin.getLogger().severe("Player " + user.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                            } else {
                                maxSize = Math.max(maxSize, Integer.valueOf(spl[1]));
                            }
                        }
                    }
                }
                // Do some sanity checking
                if (maxSize < 1) maxSize = 1;
            }
            
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", "[number]", String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        return true;
    }

}
