package world.bentobox.bentobox.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * Reads one line of chat from a player without using Bukkit conversations.
 * <p>
 * Lophine/Folia throws {@link UnsupportedOperationException} for every chat message sent while a
 * Bukkit conversation is active ({@code ServerGamePacketListenerImpl.broadcastChatMessage}), and
 * the chat event is never fired first, so a conversation can never receive input there: the
 * player just loses chat until they relog. This consumes a single message from the normal chat
 * pipeline instead, so the player never enters the conversing state.
 */
public final class ChatInput {

    private record Pending(String prompt, @Nullable String cancelWord, Consumer<String> onInput,
            @Nullable Runnable onCancel) {
    }

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private ChatInput() {
    }

    /**
     * Asks the player for one line of chat.
     *
     * @param user    player to ask
     * @param prompt  text shown to the player
     * @param onInput called with the message on the player's own thread
     */
    public static void ask(User user, String prompt, Consumer<String> onInput) {
        ask(user, prompt, null, onInput);
    }

    /**
     * Asks the player for one line of chat.
     *
     * @param user       player to ask
     * @param prompt     text shown to the player
     * @param cancelWord message that cancels the request instead of answering it, or null
     * @param onInput    called with the message on the player's own thread
     */
    public static void ask(User user, String prompt, @Nullable String cancelWord, Consumer<String> onInput) {
        ask(user, prompt, cancelWord, onInput, null);
    }

    /**
     * Asks the player for one line of chat.
     *
     * @param user       player to ask
     * @param prompt     text shown to the player
     * @param cancelWord message that cancels the request instead of answering it, or null
     * @param onInput    called with the message on the player's own thread
     * @param onCancel   called instead of {@code onInput} when the cancel word is used, or null
     */
    public static void ask(User user, String prompt, @Nullable String cancelWord, Consumer<String> onInput,
            @Nullable Runnable onCancel) {
        if (user == null || !user.isPlayer()) {
            return;
        }
        PENDING.put(user.getUniqueId(), new Pending(prompt, cancelWord, onInput, onCancel));
        onUser(user, () -> user.sendRawMessage(prompt));
    }

    /** @return true if this player owes a chat answer */
    public static boolean isWaiting(UUID uuid) {
        return PENDING.containsKey(uuid);
    }

    /** Forgets any pending question for this player. */
    public static void cancel(User user) {
        if (user != null) {
            PENDING.remove(user.getUniqueId());
        }
    }

    /**
     * Consumes the message for a player, if one was expected.
     * <p>
     * Internal: the chat listener calls this. Exposed so tests can drive the prompt without a
     * real client.
     *
     * @return true if the message belonged to this request and must not be broadcast
     */
    public static boolean consume(BentoBox plugin, User user, String message) {
        Pending pending = PENDING.remove(user.getUniqueId());
        if (pending == null) {
            return false;
        }
        String input = message == null ? "" : message.trim();
        if (pending.cancelWord() != null && input.equalsIgnoreCase(pending.cancelWord())) {
            if (pending.onCancel() != null) {
                onUser(user, pending.onCancel());
            } else {
                onUser(user, () -> user.sendMessage("general.errors.command-cancelled"));
            }
            return true;
        }
        onUser(user, () -> pending.onInput().accept(input));
        return true;
    }

    /** Player messages may only be sent from the player's own thread. */
    private static void onUser(User user, Runnable action) {
        TaskScheduler.user(BentoBox.getInstance(), user, action);
    }

    /** Listener that routes the next chat message of a waiting player to {@link #consume}. */
    public static class ChatListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onChat(AsyncChatEvent e) {
            User user = User.getInstance(e.getPlayer());
            if (!PENDING.containsKey(user.getUniqueId())) {
                return;
            }
            e.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(e.message());
            consume(BentoBox.getInstance(), user, message);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            PENDING.remove(e.getPlayer().getUniqueId());
        }
    }
}
