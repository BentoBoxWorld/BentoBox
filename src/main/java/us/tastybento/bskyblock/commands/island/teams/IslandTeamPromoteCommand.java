package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamPromoteCommand extends AbstractIslandTeamCommand {

    public IslandTeamPromoteCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "promote");
    }
    
    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setParameters("commands.island.team.promote.parameters");
        this.setDescription("commands.island.team.promote.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        return true;
    }

}