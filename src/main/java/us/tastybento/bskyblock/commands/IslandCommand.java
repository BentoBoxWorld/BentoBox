package us.tastybento.bskyblock.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.island.IslandAboutCommand;
import us.tastybento.bskyblock.commands.island.IslandCreateCommand;
import us.tastybento.bskyblock.commands.island.IslandGoCommand;
import us.tastybento.bskyblock.commands.island.IslandResetCommand;
import us.tastybento.bskyblock.commands.island.IslandResetnameCommand;
import us.tastybento.bskyblock.commands.island.IslandSethomeCommand;
import us.tastybento.bskyblock.commands.island.IslandSetnameCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamCommand;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Constants.ISLANDCOMMAND, "is");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        this.setDescription("commands.island.help.description");
        this.setOnlyPlayer(true);
        // Permission
        this.setPermission(Constants.PERMPREFIX + "island");
        // Set up subcommands
        new IslandAboutCommand(this);
        new IslandCreateCommand(this);
        new IslandGoCommand(this);
        new IslandResetCommand(this);
        new IslandSetnameCommand(this);
        new IslandResetnameCommand(this);
        new IslandSethomeCommand(this);
        // Team commands
        new IslandTeamCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // If this player does not have an island, create one
        if (!getPlugin().getIslands().hasIsland(user.getUniqueId())) {
            Optional<CompositeCommand> subCreate = this.getSubCommand("create");
            if (subCreate.isPresent()) {
                subCreate.get().execute(user, new ArrayList<>());
            }
        }
        Optional<CompositeCommand> go = this.getSubCommand("go");
        // Otherwise, currently, just go home
        if (go.isPresent()) {
            go.get().execute(user, new ArrayList<>());
        }
        
        return true;
    }
    

}
