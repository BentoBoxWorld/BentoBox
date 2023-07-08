package world.bentobox.bentobox.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Displays permissions that have been set by BentoBox.
 *
 * @author tastybento
 */
public class BentoBoxPermsCommand extends CompositeCommand {

    /**
     * Info command
     * @param parent - command parent
     */
    public BentoBoxPermsCommand(CompositeCommand parent) {
        super(parent, "perms");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.perms");
        setParametersHelp("commands.bentobox.perms.parameters");
        setDescription("commands.bentobox.perms.description");
        this.setOnlyConsole(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Loop all the known top-level commands
        getPlugin().getCommandsManager().getCommands().values().stream().distinct().forEach(cc -> {
            if (cc.getAddon() == null) {
                user.sendMessage("*** BentoBox effective perms:");
            } else if (cc.getAddon() instanceof GameModeAddon gma) {
                user.sendRawMessage("**** " + gma.getDescription().getName() + " effective perms:");
            } else {
                user.sendRawMessage("**** " + cc.getAddon().getDescription().getName() + " effective perms:");
            }
            user.sendRawMessage("permissions:");
            printData(user, cc, cc.getLabel());
            printSubCommandData(user, cc, cc.getLabel());
        });
        return true;
    }

    private void printData(User user, CompositeCommand cc, String label) {
        if (cc.getPermission().isBlank()) return;
        String desc = user.getTranslation(cc.getWorld(), cc.getDescription());
        user.sendRawMessage("  " + cc.getPermission() + ":");
        user.sendRawMessage("    description: Allow use of '/" + label + "' command - " + desc);
        Permission p = Bukkit.getPluginManager().getPermission(cc.getPermission());
        if (p != null) {
            user.sendRawMessage("    default: " + p.getDefault().name());
        } else {
            user.sendRawMessage("    default: OP"); // If not def
        }

    }

    /**
     * Iterates over sub-commands
     * @param user user
     * @param parent parent command
     * @param label label
     */
    private void printSubCommandData(User user, CompositeCommand parent, String label) {
        for (CompositeCommand cc : parent.getSubCommands().values()) {
            if (cc.getLabel().equalsIgnoreCase("help")) continue; // Ignore the help command
            String newLabel = label + " " + cc.getLabel();
            printData(user, cc, newLabel);
            printSubCommandData(user, cc, newLabel);
        }

    }
}
