package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.ChatInput;
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
    public boolean canExecute(@NonNull User user, String label, List<String> args) {
        return findIsland(user, args) != null;
    }

    private Island findIsland(User user, List<String> args) {
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return null;
        }
        Island island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return null;
        }
        // Check if the name is known
        if (!getIslands().isHomeLocation(island, String.join(" ", args))) {
            user.sendMessage("commands.island.go.unknown-home");
            user.sendMessage("commands.island.sethome.homes-are");
            island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendRawMessage(
                    user.getTranslation("commands.island.sethome.home-list-syntax", TextVariables.NAME, s)
                            + "[run_command: /" + getTopLabel() + " go " + s + "]"
                            + "[hover: " + user.getTranslation("commands.island.sethome.click-to-teleport") + "]"));
            this.showHelp(this, user);
            return null;
        }

        // check command permission
        int rank = island.getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return null;
        }

        return island;
    }

    /**
     * Asks for the new home name in chat.
     * <p>
     * Chat input is used instead of a Bukkit conversation: on Folia/Lophine a conversation
     * swallows the player's chat entirely, so the name could never be entered.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        Island island = findIsland(user, args);
        if (island == null) return false;
        String oldName = String.join(" ", args);
        ChatInput.ask(user, user.getTranslation("commands.island.renamehome.enter-new-name"), input -> {
            if (!island.equals(getIslands().getIsland(island.getWorld(), user))) {
                user.sendMessage("general.errors.no-island");
                return;
            }
            int rank = island.getRank(user);
            if (rank < island.getRankCommand(getUsage())) {
                user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                        user.getTranslation(RanksManager.getInstance().getRank(rank)));
                return;
            }
            user.sendMessage(island.renameHome(oldName, input) ? "general.success"
                    : "commands.island.renamehome.already-exists");
        });
        return true;
    }


    /**
     * Provides tab completion for existing home names.
     * Only shows homes for the user's current island.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        Island is = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (is != null) {
            return Optional.of(Util.tabLimit(new ArrayList<>(is.getHomes().keySet()), lastArg));
        } else {
            return Optional.empty();
        }
    }
}
