package world.bentobox.bentobox.api.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests the prompts API: reading and checking answers, and the chat fallback.
 * <p>
 * A dialog cannot be built under MockBukkit (see {@code DialogBuilderTest}), so the dialog
 * itself is exercised on a live server. Here the dialog setting is off unless a test says
 * otherwise, which sends every prompt down the chat path. Translations come back as their
 * locale key, so the cancel, done and confirm words are the keys.
 */
class PromptsTest extends CommonTestSetup {

    private static final String CANCEL = "prompts.chat.cancel-word";
    private static final String DONE = "prompts.chat.done-word";
    private static final String CONFIRM = "prompts.chat.confirm-word";

    private User user;
    private final AtomicInteger cancelled = new AtomicInteger();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = User.getInstance(mockPlayer);
        plugin.getSettings().setDialogPrompts(false);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        ChatInputListener.stop(uuid);
        super.tearDown();
    }

    private void say(String text) {
        ChatInputListener.accept(uuid, text);
    }

    // ------------------------------------------------------------------
    // Reading and checking answers
    // ------------------------------------------------------------------

    @Test
    void testTextIsStripped() {
        AtomicReference<String> got = new AtomicReference<>();
        assertNull(Prompts.text(user).onSubmit(got::set).submit("  hello world  "));
        assertEquals("hello world", got.get());
    }

    @Test
    void testTextRefusesEmpty() {
        AtomicReference<String> got = new AtomicReference<>();
        assertEquals("prompts.errors.empty", Prompts.text(user).onSubmit(got::set).submit("   "));
        assertNull(got.get());
    }

    @Test
    void testOptionalTextAcceptsEmpty() {
        AtomicReference<String> got = new AtomicReference<>();
        assertNull(Prompts.text(user).optional().onSubmit(got::set).submit(""));
        assertEquals("", got.get());
    }

    @Test
    void testTooLong() {
        AtomicReference<String> got = new AtomicReference<>();
        InputPrompt<String> prompt = Prompts.text(user).maxLength(3).onSubmit(got::set);
        assertEquals("prompts.errors.too-long", prompt.submit("abcd"));
        assertNull(got.get());
        assertNull(prompt.submit("abc"));
    }

    @Test
    void testMaxLengthMustBePositive() {
        InputPrompt<String> prompt = Prompts.text(user);
        assertThrows(IllegalArgumentException.class, () -> prompt.maxLength(0));
    }

    @Test
    void testChecksRunInOrderAndFirstObjectionWins() {
        AtomicReference<String> got = new AtomicReference<>();
        InputPrompt<String> prompt = Prompts.text(user).validate(s -> s.startsWith("a") ? null : "must start with a")
                .validate(s -> s.length() > 2 ? null : "too short").onSubmit(got::set);
        assertEquals("must start with a", prompt.submit("b"));
        assertEquals("too short", prompt.submit("ab"));
        assertNull(got.get());
        assertNull(prompt.submit("abc"));
        assertEquals("abc", got.get());
    }

    @Test
    void testIdIsSanitized() {
        AtomicReference<String> got = new AtomicReference<>();
        assertNull(Prompts.id(user).onSubmit(got::set).submit("My Fancy:Name"));
        assertEquals("my_fancy_name", got.get());
    }

    @Test
    void testIdOfOnlyColorCodesIsRefused() {
        AtomicReference<String> got = new AtomicReference<>();
        assertEquals("prompts.errors.empty", Prompts.id(user).onSubmit(got::set).submit("&c&l"));
        assertNull(got.get());
    }

    @Test
    void testInteger() {
        AtomicReference<Integer> got = new AtomicReference<>();
        InputPrompt<Integer> prompt = Prompts.integer(user, 1, 5000).onSubmit(got::set);
        assertEquals("prompts.errors.not-a-number", prompt.submit("lots"));
        assertEquals("prompts.errors.not-a-number", prompt.submit("2.5"));
        assertEquals("prompts.errors.out-of-range", prompt.submit("0"));
        assertEquals("prompts.errors.out-of-range", prompt.submit("5001"));
        assertEquals("prompts.errors.out-of-range", prompt.submit("99999999999"));
        assertNull(got.get());
        assertNull(prompt.submit(" 1,000 "));
        assertEquals(1000, got.get());
    }

    @Test
    void testDecimal() {
        AtomicReference<Double> got = new AtomicReference<>();
        InputPrompt<Double> prompt = Prompts.decimal(user, 0, 1).onSubmit(got::set);
        assertEquals("prompts.errors.not-a-number", prompt.submit("half"));
        assertEquals("prompts.errors.not-a-number", prompt.submit("NaN"));
        assertEquals("prompts.errors.out-of-range", prompt.submit("1.5"));
        assertNull(prompt.submit("0,25"));
        assertEquals(0.25, got.get());
    }

    @Test
    void testLinesDropTrailingBlanks() {
        AtomicReference<List<String>> got = new AtomicReference<>();
        assertNull(Prompts.lines(user).onSubmit(got::set).submit("one\n\ntwo\r\n  \n"));
        assertEquals(List.of("one", "", "two"), got.get());
    }

    @Test
    void testLinesAcceptNothing() {
        AtomicReference<List<String>> got = new AtomicReference<>();
        assertNull(Prompts.lines(user).onSubmit(got::set).submit(""));
        assertEquals(List.of(), got.get());
    }

    // ------------------------------------------------------------------
    // Asking in chat
    // ------------------------------------------------------------------

    @Test
    void testChatAnswerIsDelivered() {
        AtomicReference<String> got = new AtomicReference<>();
        Prompts.text(user).title(Component.text("Name?")).onSubmit(got::set).onCancel(cancelled::incrementAndGet)
                .open();
        assertTrue(ChatInputListener.isWaitingFor(uuid));
        verify(mockPlayer).sendMessage(Component.text("Name?"));
        checkSpigotMessage("prompts.chat.instructions");

        say("Bob");

        assertEquals("Bob", got.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
        assertEquals(0, cancelled.get());
    }

    @Test
    void testChatBadAnswerAsksAgain() {
        AtomicReference<Integer> got = new AtomicReference<>();
        Prompts.integer(user, 1, 10).onSubmit(got::set).open();

        say("eleven");

        assertNull(got.get());
        assertTrue(ChatInputListener.isWaitingFor(uuid));
        checkSpigotMessage("prompts.errors.not-a-number");

        say("7");

        assertEquals(7, got.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }

    @Test
    void testChatCancelWord() {
        AtomicReference<String> got = new AtomicReference<>();
        Prompts.text(user).onSubmit(got::set).onCancel(cancelled::incrementAndGet).open();

        say(CANCEL.toUpperCase());

        assertNull(got.get());
        assertEquals(1, cancelled.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
        checkSpigotMessage("general.errors.command-cancelled");
    }

    @Test
    void testChatLinesCollectUntilDone() {
        AtomicReference<List<String>> got = new AtomicReference<>();
        Prompts.lines(user).onSubmit(got::set).open();
        checkSpigotMessage("prompts.chat.lines-instructions");

        say("first");
        say("second");
        assertNull(got.get());
        assertTrue(ChatInputListener.isWaitingFor(uuid));

        say(DONE);

        assertEquals(List.of("first", "second"), got.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }

    @Test
    void testChatLinesStartAgainAfterAnError() {
        AtomicReference<List<String>> got = new AtomicReference<>();
        Prompts.lines(user).maxLength(5).onSubmit(got::set).open();

        say("much too long");
        say(DONE);
        assertNull(got.get());
        assertTrue(ChatInputListener.isWaitingFor(uuid));

        say("ok");
        say(DONE);
        assertEquals(List.of("ok"), got.get());
    }

    @Test
    void testChatTimeoutCancels() {
        Prompts.text(user).onCancel(cancelled::incrementAndGet).open();
        ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskLater(eq(plugin), timeout.capture(), eq(ChatInputListener.TIMEOUT_SECONDS * 20L));

        timeout.getValue().run();

        assertEquals(1, cancelled.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
        checkSpigotMessage("prompts.chat.timed-out");
    }

    @Test
    void testChatTimeoutRestartsAfterEachMessage() {
        BukkitTask first = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(first);
        Prompts.integer(user, 1, 10).open();

        say("nope");

        verify(first).cancel();
        verify(sch, times(2)).runTaskLater(eq(plugin), any(Runnable.class), anyLong());
    }

    @Test
    void testNewPromptReplacesWaitingOneWithoutCancellingIt() {
        AtomicReference<String> first = new AtomicReference<>();
        AtomicReference<String> second = new AtomicReference<>();
        Prompts.text(user).onSubmit(first::set).onCancel(cancelled::incrementAndGet).open();
        Prompts.text(user).onSubmit(second::set).open();

        say("answer");

        assertNull(first.get());
        assertEquals("answer", second.get());
        assertEquals(0, cancelled.get());
    }

    @Test
    void testPromptOpenedFromOnSubmitKeepsWaiting() {
        List<String> got = new ArrayList<>();
        Prompts.text(user).onSubmit(a -> {
            got.add(a);
            Prompts.text(user).onSubmit(got::add).open();
        }).open();

        say("one");
        assertTrue(ChatInputListener.isWaitingFor(uuid));
        say("two");

        assertEquals(List.of("one", "two"), got);
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }

    @Test
    void testNotAPlayerIsNotAsked() {
        User console = mock(User.class);
        when(console.isPlayer()).thenReturn(false);
        when(console.getTranslation(anyString())).thenReturn("");
        Prompts.text(console).open();
        Prompts.confirm(console).open();
        verify(sch, never()).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    @Test
    void testDialogThatCannotBeBuiltFallsBackToChat() {
        plugin.getSettings().setDialogPrompts(true);
        assertTrue(Prompts.usesDialog(user));
        AtomicReference<String> got = new AtomicReference<>();

        Prompts.text(user).onSubmit(got::set).open();

        // MockBukkit has no dialog registry, which stands in for any failure to show one
        verify(plugin).logError(anyString());
        assertTrue(ChatInputListener.isWaitingFor(uuid));
        say("still works");
        assertEquals("still works", got.get());
    }

    @Test
    void testUsesDialogFollowsSetting() {
        assertFalse(Prompts.usesDialog(user));
        plugin.getSettings().setDialogPrompts(true);
        assertTrue(Prompts.usesDialog(user));
    }

    // ------------------------------------------------------------------
    // Confirmation
    // ------------------------------------------------------------------

    @Test
    void testConfirmInChat() {
        AtomicInteger confirmed = new AtomicInteger();
        Prompts.confirm(user).message(Component.text("Wipe it all")).onConfirm(confirmed::incrementAndGet)
                .onCancel(cancelled::incrementAndGet).open();
        verify(mockPlayer).sendMessage(Component.text("Wipe it all"));
        checkSpigotMessage("prompts.chat.confirm-instructions");

        say(CONFIRM);

        assertEquals(1, confirmed.get());
        assertEquals(0, cancelled.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }

    @Test
    void testAnythingElseDeclines() {
        AtomicInteger confirmed = new AtomicInteger();
        Prompts.confirm(user).onConfirm(confirmed::incrementAndGet).onCancel(cancelled::incrementAndGet).open();

        say("er, no");

        assertEquals(0, confirmed.get());
        assertEquals(1, cancelled.get());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
        checkSpigotMessage("general.errors.command-cancelled");
    }

    // ------------------------------------------------------------------
    // The listener
    // ------------------------------------------------------------------

    @Test
    void testChatEventIsTakenAsTheAnswer() {
        AtomicReference<String> got = new AtomicReference<>();
        Prompts.text(user).onSubmit(got::set).open();
        AsyncChatEvent e = mock(AsyncChatEvent.class);
        when(e.getPlayer()).thenReturn(mockPlayer);
        when(e.message()).thenReturn(Component.text("  typed in chat "));

        new ChatInputListener().onChat(e);

        verify(e).setCancelled(true);
        // Chat is async: nothing is delivered until the main thread task runs
        assertNull(got.get());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTask(eq(plugin), task.capture());
        task.getValue().run();
        assertEquals("typed in chat", got.get());
    }

    @Test
    void testChatEventIsLeftAloneWithoutAPrompt() {
        AsyncChatEvent e = mock(AsyncChatEvent.class);
        when(e.getPlayer()).thenReturn(mockPlayer);

        new ChatInputListener().onChat(e);

        verify(e, never()).setCancelled(true);
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    @Test
    void testQuitForgetsThePrompt() {
        Prompts.text(user).onCancel(cancelled::incrementAndGet).open();
        PlayerQuitEvent e = mock(PlayerQuitEvent.class);
        when(e.getPlayer()).thenReturn(mockPlayer);

        new ChatInputListener().onQuit(e);

        assertFalse(ChatInputListener.isWaitingFor(uuid));
        assertEquals(0, cancelled.get());
    }
}
