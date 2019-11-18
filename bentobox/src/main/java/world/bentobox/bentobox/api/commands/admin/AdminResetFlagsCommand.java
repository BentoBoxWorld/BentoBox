package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Admin command to reset all islands in a world to the default flag setting in the game mode config.yml
 * @author tastybento
 * @since 1.3.0
 */
public class AdminResetFlagsCommand extends ConfirmableCommand {

    private List<String> options;

    public AdminResetFlagsCommand(CompositeCommand parent) {
        super(parent, "resetflags");
        options = getPlugin().getFlagsManager().getFlags().stream()
                .filter(f -> f.getType().equals(Type.PROTECTION) || f.getType().equals(Type.SETTING))
                .map(Flag::getID).collect(Collectors.toList());
    }

    @Override
    public void setup() {
        setPermission("admin.resetflags");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.resetflags.parameters");
        setDescription("commands.admin.resetflags.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            askConfirmation(user, user.getTranslation("commands.admin.resetflags.confirm"), () -> {
                getIslands().resetAllFlags(getWorld());
                user.sendMessage("commands.admin.resetflags.success");
            });
            return true;
        } else if (args.size() == 1 && options.contains(args.get(0).toUpperCase(Locale.ENGLISH))) {
            getPlugin().getFlagsManager().getFlag(args.get(0).toUpperCase(Locale.ENGLISH)).ifPresent(flag ->
            askConfirmation(user, user.getTranslation("commands.admin.resetflags.confirm"), () -> {
                getIslands().resetFlag(getWorld(), flag);
                user.sendMessage("commands.admin.resetflags.success-one", TextVariables.NAME, flag.getID());
            }));
            return true;
        }
        // Show help
        showHelp(this, user);
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
