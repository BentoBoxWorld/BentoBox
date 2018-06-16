package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.panels.SettingsPanel;
import us.tastybento.bskyblock.util.Util;

/**
 * World settings command
 * @author tastybento
 */
public class AdminWorldCommand extends CompositeCommand {

    public AdminWorldCommand(CompositeCommand islandCommand) {
        super(islandCommand, "world");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setPermission("admin.world");
        setOnlyPlayer(true);
        setDescription("commands.admin.world.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // Settings are only shown if you are in the right world
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            SettingsPanel.openPanel(getPlugin(), user, Flag.Type.WORLD_SETTING);
            return true;
        } else {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
    }
}
