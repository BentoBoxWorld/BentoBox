package us.tastybento.bskyblock.commands.island;

import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.panels.LanguagePanel;

/**
 * @author Poslovitch
 */
public class IslandLanguageCommand extends CompositeCommand {

    public IslandLanguageCommand(IslandCommand islandCommand) {
        super(islandCommand, "language", "lang");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.language");
        setOnlyPlayer(true);
        setDescription("commands.island.language.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        LanguagePanel.openPanel(user);
        return true;
    }
}