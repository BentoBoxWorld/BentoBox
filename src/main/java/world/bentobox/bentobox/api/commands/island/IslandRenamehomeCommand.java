package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.conversations.ConversationFactory;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.commands.admin.conversations.NamePrompt;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island rename home command (/island renamehome).
 * <p>
 * This command allows players to rename their existing home locations through
 * an interactive conversation prompt.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement (default: Member)</li>
 *   <li>Interactive name input via conversation</li>
 *   <li>Tab completion for existing home names</li>
 *   <li>Home name validation</li>
 *   <li>90-second conversation timeout</li>
 * </ul>
 * <p>
 * Usage: /island renamehome &lt;current name&gt;
 * <br>
 * Permission: {@code island.renamehome}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandRenamehomeCommand extends ConfirmableCommand {

    /**
     * Cached island instance to avoid multiple database lookups.
     * Set during canExecute and used in execute.
     */
    private @Nullable Island island;

    public IslandRenamehomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "renamehome");
    }

    @Override
    public void setup() {
        setPermission("island.renamehome");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.renamehome.parameters");
        setDescription("commands.island.renamehome.description");
        setConfigurableRankCommand();
        setDefaultCommandRank(RanksManager.MEMBER_RANK);
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Arguments provided</li>
     *   <li>Player has an island</li>
     *   <li>Home name exists</li>
     *   <li>Player has sufficient rank</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check if the name is known
        if (!getIslands().isHomeLocation(island, String.join(" ", args))) {
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
            this.showHelp(this, user);
            return false;
        }

        // check command permission
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        return true;
    }

    /**
     * Starts an interactive conversation to rename the home.
     * <p>
     * Creates a conversation with:
     * <ul>
     *   <li>Modal dialog (blocks other chat)</li>
     *   <li>No local echo of input</li>
     *   <li>90-second timeout</li>
     *   <li>Custom name prompt</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        new ConversationFactory(BentoBox.getInstance())
        .withModality(true)
        .withLocalEcho(false)
        .withTimeout(90)
        .withFirstPrompt(new NamePrompt(getPlugin(), island, user, String.join(" ", args)))
        .buildConversation(user.getPlayer()).begin();
        return true;
    }


    /**
     * Provides tab completion for existing home names.
     * Only shows homes for the user's current island.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        Island is = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (is != null) {
            return Optional.of(Util.tabLimit(new ArrayList<>(is.getHomes().keySet()), lastArg));
        } else {
            return Optional.empty();
        }
    }
}
