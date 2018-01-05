/**
 * 
 */
package us.tastybento.bskyblock.commands.admin;

import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.ConfigLoader;

/**
 * @author ben
 *
 */
public class AdminReloadCommand extends CompositeCommand {

    /**
     * @param parent
     * @param label
     * @param aliases
     */
    public AdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.commands.User, java.util.List)
     */
    @Override
    public boolean execute(User user, List<String> args) {
        new ConfigLoader();
        return true;
    }

}
