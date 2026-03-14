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

    /**
     * Collects all help entries including addon-registered top-level commands.
     */
    @Override
    protected List<String[]> getHelpEntries(User user) {
        List<String[]> entries = super.getHelpEntries(user);
        // Add registered addon commands
        getPlugin().getCommandsManager().getCommands().values().stream()
                .filter(cc -> cc.getAddon() != null)
                .sorted(Comparator.comparing(CompositeCommand::getName))
                .forEach(v -> {
                    String params = user.getTranslationOrNothing(v.getParameters());
                    String desc = user.getTranslationOrNothing(v.getDescription()) + " (" + v.getAddon().getDescription().getName() + ")";
                    entries.add(new String[]{v.getUsage(), params, desc});
                });
        return entries;
    }
}