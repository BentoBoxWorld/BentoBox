package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;

/**
 * @since 1.5.0
 * @author tastybento
 */
public class AdminSwitchCommand extends ConfirmableCommand {

    /**
     * Switches bypass on and off
     * @param parent - admin command
     * @since 1.5.0
     */
    public AdminSwitchCommand(CompositeCommand parent) {
        super(parent, "switch");
    }

    @Override
    public void setup() {
        setPermission("mod.switch");
        setOnlyPlayer(true);
        setParametersHelp("commands.admin.switch.parameters");
        setDescription("commands.admin.switch.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        boolean switchState = user.getMetaData("AdminCommandSwitch").map(MetaDataValue::asBoolean).orElse(false);
        if (switchState) {
            // Turn off
            user.putMetaData("AdminCommandSwitch", new MetaDataValue(false));
            user.sendMessage("commands.admin.switch.adding"); // Adding protection bypass
            user.sendMessage("general.success");
        } else {
            // Turn on
            user.putMetaData("AdminCommandSwitch", new MetaDataValue(true));
            user.sendMessage("commands.admin.switch.removing"); // Removing protection bypass
            user.sendMessage("general.success");
        }
        return true;
    }

}
