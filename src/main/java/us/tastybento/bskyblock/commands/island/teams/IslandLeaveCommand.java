package us.tastybento.bskyblock.commands.island.teams;

import java.util.Set;

import us.tastybento.bskyblock.api.commands.User;

public class IslandLeaveCommand extends AbstractIslandTeamCommandArgument {

    public IslandLeaveCommand() {
        super("leave");
    }

    @Override
    public boolean execute(User user, String[] args) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }


}