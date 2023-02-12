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
 * Renames a home
 * @author tastybento
 *
 */
public class IslandRenamehomeCommand extends ConfirmableCommand {

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
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        return true;
    }

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
