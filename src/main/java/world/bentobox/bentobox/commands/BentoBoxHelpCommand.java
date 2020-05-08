package world.bentobox.bentobox.commands;

import java.util.Comparator;
import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DefaultHelpCommand;
import world.bentobox.bentobox.api.user.User;

/**
 * Lists help and commands registered by addons BentoBox.
 *
 * @author tastybento
 * @since 1.14.0
 */
public class BentoBoxHelpCommand extends DefaultHelpCommand {

    /**
     * Custom help to show addon registered top level commands
     * @param parent command parent
     */
    public BentoBoxHelpCommand(CompositeCommand parent) {
        super(parent);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        super.execute(user, label, args);
        // Show registered addon commands
        getPlugin().getCommandsManager().getCommands().values().stream()
        .filter(cc -> cc.getAddon() != null)
        .sorted(Comparator.comparing(CompositeCommand::getName))
        .forEach(v -> showPrettyHelp(user, v.getUsage(),
                user.getTranslationOrNothing(v.getParameters()),
                user.getTranslationOrNothing(v.getDescription()) + " (" + v.getAddon().getDescription().getName() + ")"));
        user.sendMessage("commands.help.end");
        return true;
    }
}