package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.user.User;

public class NameConversationPrefix implements ConversationPrefix {

    @Override
    public String getPrefix(ConversationContext context) {
        User user = User.getInstance((Player)context.getForWhom());
        return user.getTranslation("commands.admin.blueprint.management.name.conversation-prefix");
    }
}
