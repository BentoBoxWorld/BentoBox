package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.customizable.LanguagePanel;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island language command (/island language).
 * <p>
 * This command allows players to change their personal language settings.
 * It can be used either with a language code argument or through a GUI panel.
 * <p>
 * Features:
 * <ul>
 *   <li>GUI-based language selection</li>
 *   <li>Direct language code input</li>
 *   <li>Tab completion for available languages</li>
 *   <li>Persistent language settings</li>
 * </ul>
 * <p>
 * Usage:
 * <ul>
 *   <li>/island language - Opens language selection GUI</li>
 *   <li>/island language &lt;code&gt; - Directly sets language</li>
 * </ul>
 * <p>
 * Permission: {@code island.language}
 * Aliases: language, lang
 *
 * @author Poslovitch
 * @since 1.0
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

    /**
     * Handles language selection through GUI or direct command.
     * <p>
     * Flow:
     * <ul>
     *   <li>With argument: Attempts to set language directly</li>
     *   <li>Without argument: Opens language selection GUI</li>
     * </ul>
     * <p>
     * Validates:
     * <ul>
     *   <li>Language availability</li>
     *   <li>Not already selected</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // The user provided a language code
            Locale locale = Locale.forLanguageTag(args.getFirst());
            if (getPlugin().getLocalesManager().isLocaleAvailable(locale)) {
                // Check if that locale is not already selected
                if (!user.getLocale().equals(locale)) {
                    getPlugin().getPlayers().setLocale(user.getUniqueId(), locale.toLanguageTag());
                    user.sendMessage("panels.language.edited", "[lang]", locale.toLanguageTag());
                } else {
                    user.sendMessage("commands.island.language.already-selected");
                    return false;
                }
            } else {
                user.sendMessage("commands.island.language.not-available");
                return false;
            }
        } else {
            LanguagePanel.openPanel(this, user);
        }
        return true;
    }

    /**
     * Provides tab completion for available language codes.
     * Lists all languages that are currently available in the plugin.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        for (Locale locale : getPlugin().getLocalesManager().getAvailableLocales(true)) {
            options.add(locale.toLanguageTag());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}