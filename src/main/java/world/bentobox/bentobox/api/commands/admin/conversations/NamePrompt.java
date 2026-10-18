package world.bentobox.bentobox.api.commands.admin.conversations;

import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Renames a home
 * @author tastybento
 *
 */
public class NamePrompt extends StringPrompt {

    @NonNull
    private final Island island;
    @NonNull
    private final User user;
    private final String oldName;
    private final BentoBox plugin;
    private final String commandUsage;

    public NamePrompt(BentoBox plugin, @NonNull Island island, @NonNull User user, String oldName) {
        this(plugin, island, user, oldName, null);
    }

    public NamePrompt(BentoBox plugin, @NonNull Island island, @NonNull User user, String oldName, String commandUsage) {
        this.plugin = plugin;
        this.island = island;
        this.user = user;
        this.oldName = oldName;
        this.commandUsage = commandUsage;
    }

    @Override
    @NonNull
    public String getPromptText(@NonNull ConversationContext context) {
        return user.getTranslation("commands.island.renamehome.enter-new-name");
    }

    @Override
    public Prompt acceptInput(@NonNull ConversationContext context, String input) {
        TaskScheduler.user(plugin, user, () -> {
            if (!island.equals(plugin.getIslands().getIsland(island.getWorld(), user))) {
                user.sendMessage("general.errors.no-island");
                return;
            }
            int rank = island.getRank(user);
            int requiredRank = commandUsage == null ? RanksManager.MEMBER_RANK : island.getRankCommand(commandUsage);
            if (rank < requiredRank) {
                user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                        user.getTranslation(RanksManager.getInstance().getRank(rank)));
                return;
            }
            user.sendMessage(island.renameHome(oldName, input) ? "general.success"
                    : "commands.island.renamehome.already-exists");
        });
        return Prompt.END_OF_CONVERSATION;
    }

}
