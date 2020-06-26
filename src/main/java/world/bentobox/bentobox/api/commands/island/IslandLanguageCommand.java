package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.LanguagePanel;
import world.bentobox.bentobox.util.Util;

/**
 * @author Poslovitch
 */
public class IslandLanguageCommand extends CompositeCommand {

    public IslandLanguageCommand(CompositeCommand islandCommand) {
        super(islandCommand, "language", "lang");
    }

    @Override
    public void setup() {
        setPermission("island.language");
        setOnlyPlayer(true);
        setDescription("commands.island.language.description");
        setParametersHelp("commands.island.language.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // The user provided a language code
            Locale locale = Locale.forLanguageTag(args.get(0));
            if (getPlugin().getLocalesManager().isLocaleAvailable(locale)) {
                // Check if that locale is not already selected
                if (!user.getLocale().equals(locale)) {
                    getPlugin().getPlayers().setLocale(user.getUniqueId(), locale.toLanguageTag());
                    user.sendMessage("language.edited", "[lang]", locale.toLanguageTag());
                } else {
                    user.sendMessage("commands.island.language.already-selected");
                    return false;
                }
            } else {
                user.sendMessage("commands.island.language.not-available");
                return false;
            }
        } else {
            LanguagePanel.openPanel(user);
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        for (Locale locale : getPlugin().getLocalesManager().getAvailableLocales(true)) {
            options.add(locale.toLanguageTag());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}