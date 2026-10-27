package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.papermc.paper.event.player.AsyncChatEvent;
import world.bentobox.bentobox.api.user.User;

class ChatInputTest {

    private User user(UUID uuid) {
        User user = mock(User.class);
        when(user.isPlayer()).thenReturn(true);
        when(user.getUniqueId()).thenReturn(uuid);
        return user;
    }

    /**
     * A single message answers the request, is trimmed, and cannot be consumed twice, so the
     * next chat line is broadcast normally.
     */
    @Test
    void consumesExactlyOneMessage() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);
        List<String> answers = new ArrayList<>();
        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class)) {
            ChatInput.ask(user, "type a name", answers::add);
            assertTrue(ChatInput.isWaiting(uuid));

            assertTrue(ChatInput.consume(null, user, "  hello  "));
            assertFalse(ChatInput.isWaiting(uuid), "the request must be gone after one message");
            assertFalse(ChatInput.consume(null, user, "second"), "a second message must not be consumed");

            ignored.verify(() -> TaskScheduler.user(any(), any(), tasks.capture()), Mockito.atLeastOnce());
            tasks.getAllValues().getLast().run();
            assertEquals(List.of("hello"), answers);
        }
    }

    /** The cancel word ends the request without running the callback. */
    @Test
    void cancelWordEndsTheRequest() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);
        List<String> answers = new ArrayList<>();

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class)) {
            ChatInput.ask(user, "type a name", "quit", answers::add);

            assertTrue(ChatInput.consume(null, user, "QUIT"));
            assertFalse(ChatInput.isWaiting(uuid));
            assertTrue(answers.isEmpty(), "cancelling must not run the callback");

            ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
            ignored.verify(() -> TaskScheduler.user(any(), any(), tasks.capture()), Mockito.atLeastOnce());
            tasks.getAllValues().forEach(Runnable::run);
            verify(user).sendMessage("general.errors.command-cancelled");
        }
    }

    /** A message from a player who was not asked is left alone. */
    @Test
    void ignoresPlayersThatWereNotAsked() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class)) {
            assertFalse(ChatInput.consume(null, user, "hello"));
        }
    }

    /** Asking twice keeps only the newest question, and cancel() clears it. */
    @Test
    void cancelForgetsTheRequest() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class)) {
            ChatInput.ask(user, "first", s -> { });
            ChatInput.ask(user, "second", s -> { });
            assertTrue(ChatInput.isWaiting(uuid));

            ChatInput.cancel(user);
            assertFalse(ChatInput.isWaiting(uuid));
            assertFalse(ChatInput.consume(null, user, "hello"));
        }
    }

    /** The message that answers the question must never be broadcast. */
    @Test
    void listenerCancelsTheAnswerMessage() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(net.kyori.adventure.text.Component.text("hello"));

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class);
                MockedStatic<User> users = Mockito.mockStatic(User.class)) {
            users.when(() -> User.getInstance(player)).thenReturn(user);
            ChatInput.ask(user, "type a name", s -> { });

            new ChatInput.ChatListener().onChat(event);

            verify(event).setCancelled(true);
            assertFalse(ChatInput.isWaiting(uuid));
        }
    }

    /** Chat from a player who was not asked must be left alone. */
    @Test
    void listenerLeavesOtherChatAlone() {
        UUID uuid = UUID.randomUUID();
        User user = user(uuid);
        org.bukkit.entity.Player player = mock(org.bukkit.entity.Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);

        try (MockedStatic<TaskScheduler> ignored = Mockito.mockStatic(TaskScheduler.class);
                MockedStatic<User> users = Mockito.mockStatic(User.class)) {
            users.when(() -> User.getInstance(player)).thenReturn(user);

            new ChatInput.ChatListener().onChat(event);

            verify(event, org.mockito.Mockito.never()).setCancelled(true);
        }
    }
}
