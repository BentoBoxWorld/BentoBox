package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;

public class NameConversationPrefix implements ConversationPrefix {

    @Override
    public String getPrefix(ConversationContext conversationContext) {
        return ChatColor.GOLD + "> ";
    }
}
