package us.tastybento.bskyblock.commands.island.teams;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class IslandTeamPromoteCommand extends AbstractIslandTeamCommand {

    public IslandTeamPromoteCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "promote");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setUsage("island.team.promote.usage");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        return null;
    }

}