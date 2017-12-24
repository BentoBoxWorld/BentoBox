package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class IslandLeaveCommand extends AbstractIslandTeamCommand {

    public IslandLeaveCommand(CompositeCommand islandCommand) {
        super(islandCommand, "leave");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);

    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO Auto-generated method stub
        return false;
    }

}