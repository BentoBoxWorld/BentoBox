package us.tastybento.bskyblock.commands.island;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.panels.SettingsPanel;
import us.tastybento.bskyblock.util.Util;

/**
 * @author Poslovitch
 */
public class IslandSettingsCommand extends CompositeCommand {

    public IslandSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission("island.settings");
        setOnlyPlayer(true);
        setDescription("commands.island.settings.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // Settings are only shown if you are in the right world
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            SettingsPanel.openPanel(getPlugin(), user, Flag.Type.PROTECTION); //TODO keep track of history?
            return true;
        } else {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
    }
}
