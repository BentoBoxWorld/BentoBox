package world.bentobox.bentobox.util;

import org.bukkit.conversations.ConversationFactory;
import org.bukkit.plugin.Plugin;

/**
 * Builds chat conversations that can start on Folia.
 * <p>
 * {@link ConversationFactory#withTimeout(int)} adds an {@code InactivityConversationCanceller},
 * which calls {@code BukkitScheduler.scheduleSyncDelayedTask} when the conversation starts.
 * Folia's Bukkit scheduler throws {@link UnsupportedOperationException}, so on Folia the
 * timeout must be left unset.
 * <p>
 * Note that Lophine 26.3 rejects every chat message sent while a conversation is active
 * ({@code ServerGamePacketListenerImpl.broadcastChatMessage} throws before the chat event), so a
 * conversation started there can never receive input and leaves the player unable to chat until
 * they relog. Do not open one on Folia: use {@link ChatInput} instead.
 */
public final class Conversations {

    private Conversations() {
    }

    /**
     * @param plugin          owning plugin
     * @param modality        whether the conversation is modal
     * @param localEcho       whether the player's own messages are echoed
     * @param timeoutSeconds  inactivity timeout; ignored on Folia
     * @return the configured factory
     */
    public static ConversationFactory factory(Plugin plugin, boolean modality, boolean localEcho, int timeoutSeconds) {
        ConversationFactory factory = new ConversationFactory(plugin).withLocalEcho(localEcho);
        if (modality) {
            factory.withModality(true);
        }
        if (!TaskScheduler.isFolia()) {
            factory.withTimeout(timeoutSeconds);
        }
        return factory;
    }
}
