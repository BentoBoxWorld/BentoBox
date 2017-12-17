package us.tastybento.bskyblock.commands.admin;

import java.util.Set;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;

public class AdminVersionCommand extends CommandArgument {

    public AdminVersionCommand() {
        super("version");
    }

    @Override
    public boolean execute(User user, String[] args) {
        return false;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }
}
