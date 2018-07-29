package world.bentobox.bbox.commands.admin;

import java.util.List;

import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;

/**
 * @author tastybento
 *
 */
public class AdminReloadCommand extends CompositeCommand {

    /**
     * @param parent - parent command
     */
    public AdminReloadCommand(CompositeCommand parent) {
        super(parent, "reload", "rl");
    }

    @Override
    public void setup() {
        setDescription("commands.admin.reload.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        return true;
    }

}
