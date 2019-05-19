package world.bentobox.bentobox.blueprints.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;

import net.md_5.bungee.api.ChatColor;

public class NameConversationPrefix implements ConversationPrefix {

    @Override
    public String getPrefix(ConversationContext conversationContext) {
        return ChatColor.GOLD + "> ";
    }
}
