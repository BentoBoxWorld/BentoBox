/**
 *
 */
package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

/**
 * @author tastybento
 *
 */
public class AdminReloadCommand extends CompositeCommand {

    /**
     * @param parent
     */
    public AdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload", "rl");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        setDescription("commands.admin.reload.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)
     */
    @Override
    public boolean execute(User user, List<String> args) {
        return true;
    }

}
