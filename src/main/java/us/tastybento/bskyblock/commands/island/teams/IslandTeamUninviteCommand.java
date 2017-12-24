package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamUninviteCommand extends AbstractIslandTeamCommand {

    public IslandTeamUninviteCommand(CompositeCommand islandCommand) {
        super(islandCommand, "uninvite");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Can only use if you have an invite out there
        if(!inviteList.inverse().containsKey(playerUUID)) {
            return true;
        }

        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                        .getIsland(playerUUID))
                .reason(TeamReason.UNINVITE)
                .involvedPlayer(playerUUID)
                .build();
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return true;
        // Perform uninvite
        if (inviteList.inverse().containsKey(playerUUID)) {
            User invitee = User.getInstance(inviteList.inverse().get(playerUUID));
            if (invitee != null) {
                inviteList.inverse().remove(playerUUID);
                invitee.sendMessage("invite.nameHasUninvitedYou", "[name]", user.getName());
                user.sendMessage("general.success");
            }
        } else {
            user.sendMessage("help.island.invite");
        }
        return false;
    }

    // TODO: FIX THIS
    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = (!args.isEmpty() ? args.getLast() : "");
        if (inviteList.inverse().containsKey(user.getUniqueId())) {
            User invitee = User.getInstance(inviteList.inverse().get(user.getUniqueId()));
            if (invitee != null) {
                options.add(invitee.getName());
            }
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
