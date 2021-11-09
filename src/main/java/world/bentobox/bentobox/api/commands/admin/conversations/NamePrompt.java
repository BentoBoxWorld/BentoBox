package world.bentobox.bentobox.api.commands.admin.conversations;

import org.bukkit.Bukkit;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Renames a home
 * @author tastybento
 *
 */
public class NamePrompt extends StringPrompt {

    private @NonNull final Island island;
    private @NonNull final User user;
    private final String oldName;
    private final BentoBox plugin;

    public NamePrompt(BentoBox plugin, @NonNull Island island, @NonNull User user, String oldName) {
        this.plugin = plugin;
        this.island = island;
        this.user = user;
        this.oldName = oldName;
    }

    @Override
    public @NonNull String getPromptText(@NonNull ConversationContext context) {
        return user.getTranslation("commands.island.renamehome.enter-new-name");
    }

    @Override
    public Prompt acceptInput(@NonNull ConversationContext context, String input) {
        if (island.renameHome(oldName, input)) {
            plugin.getIslands().save(island);
            Bukkit.getScheduler().runTask(plugin, () -> user.sendMessage("general.success"));
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> user.sendMessage("commands.island.renamehome.already-exists"));
        }
        return Prompt.END_OF_CONVERSATION;
    }

}
