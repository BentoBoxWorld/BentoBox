package world.bentobox.bentobox.api.prompts;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * Collects a prompt's answer from chat, for players who are not shown a dialog. While a
 * player has a prompt waiting, what they say in chat is taken as the answer and is not
 * broadcast. Commands still work.
 * <p>
 * BentoBox registers this listener itself; prompts are opened through {@link Prompts}.
 *
 * @author tastybento
 * @since 3.25.0
 */
public class ChatInputListener implements Listener {

    /** How long a player has to answer, in seconds. The time starts again after each message. */
    public static final int TIMEOUT_SECONDS = 90;

    /**
     * A prompt waiting for a chat message.
     *
     * @param user      who is being asked
     * @param handler   given each message; returns true when the prompt is finished
     * @param onTimeout run if the player does not answer in time
     * @param timeout   the task that gives up waiting, or null if it could not be scheduled
     */
    private record Session(User user, Predicate<String> handler, Runnable onTimeout, @Nullable BukkitTask timeout) {
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    /**
     * Starts taking a player's chat messages as answers. Replaces any prompt the player
     * already has waiting, without running that prompt's timeout action.
     *
     * @param user      who to listen to
     * @param handler   given each message on the main thread; returns true when no more
     *                  messages are wanted
     * @param onTimeout run on the main thread if the player says nothing for
     *                  {@link #TIMEOUT_SECONDS} seconds
     */
    static void listen(@NonNull User user, @NonNull Predicate<String> handler, @NonNull Runnable onTimeout) {
        stop(user.getUniqueId());
        SESSIONS.put(user.getUniqueId(), new Session(user, handler, onTimeout, startTimeout(user)));
    }

    /**
     * Stops waiting for a player's answer, if a prompt is waiting.
     *
     * @param uuid the player
     */
    static void stop(@NonNull UUID uuid) {
        cancelTimeout(SESSIONS.remove(uuid));
    }

    /**
     * @param uuid a player
     * @return true if the player's next chat message will be taken as the answer to a prompt
     */
    public static boolean isWaitingFor(@NonNull UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    private static BukkitTask startTimeout(User user) {
        return Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> {
            Session session = SESSIONS.remove(user.getUniqueId());
            if (session != null) {
                user.sendMessage("prompts.chat.timed-out");
                session.onTimeout().run();
            }
        }, TIMEOUT_SECONDS * 20L);
    }

    private static void cancelTimeout(@Nullable Session session) {
        if (session != null && session.timeout() != null) {
            session.timeout().cancel();
        }
    }

    /**
     * Hands a message to the player's waiting prompt.
     *
     * @param uuid the player
     * @param text what they said
     */
    static void accept(@NonNull UUID uuid, @NonNull String text) {
        Session session = SESSIONS.get(uuid);
        if (session == null) {
            return;
        }
        boolean finished = session.handler().test(text);
        // The handler may have opened another prompt, which replaced this one already
        if (SESSIONS.get(uuid) != session) {
            return;
        }
        cancelTimeout(session);
        if (finished) {
            SESSIONS.remove(uuid);
        } else {
            SESSIONS.put(uuid, new Session(session.user(), session.handler(), session.onTimeout(),
                    startTimeout(session.user())));
        }
    }

    /**
     * Takes a chat message as the answer to a waiting prompt. Chat is asynchronous, so the
     * answer is passed to the main thread.
     *
     * @param e the chat event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!SESSIONS.containsKey(uuid)) {
            return;
        }
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).strip();
        Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> accept(uuid, text));
    }

    /**
     * Forgets a prompt when the player leaves.
     *
     * @param e the quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stop(e.getPlayer().getUniqueId());
    }
}
