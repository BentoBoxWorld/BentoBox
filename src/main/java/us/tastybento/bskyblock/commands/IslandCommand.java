package us.tastybento.bskyblock.commands;

import java.util.ArrayList;
import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.island.*;
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
        setDescription("commands.island.help.description");
        setOnlyPlayer(true);
        // Permission
        setPermission(Constants.PERMPREFIX + "island");
        // Set up subcommands
        new IslandAboutCommand(this);
        new IslandCreateCommand(this);
        new IslandGoCommand(this);
        new IslandResetCommand(this);
        new IslandSetnameCommand(this);
        new IslandResetnameCommand(this);
        new IslandSethomeCommand(this);
        new IslandSettingsCommand(this);
        // Team commands
        new IslandTeamCommand(this);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // If this player does not have an island, create one
        if (!getPlugin().getIslands().hasIsland(user.getUniqueId())) {
            getSubCommand("create").ifPresent(createCmd -> execute(user, new ArrayList<>()));
            return true;
        }
        // Otherwise, currently, just go home
        getSubCommand("go").ifPresent(goCmd -> execute(user, new ArrayList<>()));

        return true;
    }


}
