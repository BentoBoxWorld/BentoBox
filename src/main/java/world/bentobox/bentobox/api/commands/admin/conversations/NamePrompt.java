package world.bentobox.bentobox.api.commands.admin.conversations;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.localization.TextVariables;
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

    public NamePrompt(@NonNull Island island, User user, String oldName) {
        this.island = island;
        this.user = user;
        this.oldName = oldName;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return user.getTranslation("commands.island.renamehome.enter-new-name");
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (island.renameHome(oldName, input)) {
            user.getTranslation("general.success");
        } else {
            user.getTranslation("commands.island.renamehome.already-exists");
            user.sendMessage("commands.island.sethome.homes-are");
            island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("home-list-syntax", TextVariables.NAME, s));
        }
        return Prompt.END_OF_CONVERSATION;
    }

}
