package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.user.User;

/**
 * @deprecated Paper is removing {@code org.bukkit.conversations}, which this class is built on.
 *             Use the prompts in {@link world.bentobox.bentobox.panels.BlueprintManagementPanel} instead.
 */
@Deprecated(since = "3.25.0", forRemoval = true)
public class NameConversationPrefix implements ConversationPrefix {

    @Override
    public @NonNull String getPrefix(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        return user.getTranslation("commands.admin.blueprint.management.name.conversation-prefix");
    }
}
