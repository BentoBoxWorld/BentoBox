package world.bentobox.bentobox.api.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author tastybento
 *
 */
class NotifierTest {

    private Notifier n;

    @BeforeEach
    void setUp() {
        n = new Notifier();
    }

    @AfterEach
    void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testNotifyUserString() {
        User user = mock(User.class);
        String message = "a message";
        assertTrue(n.notify(user, message));
        Mockito.verify(user).sendRawMessage(message);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testNotifyUserStringMultisend() {
        User user = mock(User.class);
        String message = "a message";
        assertTrue(n.notify(user, message));
        // Spam
        for (int i = 0; i< 10; i++) {
            assertFalse(n.notify(user, message));
        }
        Mockito.verify(user).sendRawMessage(message);
    }

    /**
     * Two different messages in quick succession should both be sent - only repeats are throttled.
     */
    @Test
    void testNotifyDifferentMessagesBothSent() {
        User user = mock(User.class);
        assertTrue(n.notify(user, "message one"));
        assertTrue(n.notify(user, "message two"));
        Mockito.verify(user).sendRawMessage("message one");
        Mockito.verify(user).sendRawMessage("message two");
    }

    /**
     * Alternating between two messages must not defeat the throttle. Previously only the last
     * message was remembered, so two different repeating messages (e.g. two different limits
     * being hit back-to-back) would spam through unthrottled.
     */
    @Test
    void testNotifyAlternatingMessagesThrottled() {
        User user = mock(User.class);
        assertTrue(n.notify(user, "message one"));
        assertTrue(n.notify(user, "message two"));
        // Spam
        for (int i = 0; i < 10; i++) {
            assertFalse(n.notify(user, "message one"));
            assertFalse(n.notify(user, "message two"));
        }
        Mockito.verify(user).sendRawMessage("message one");
        Mockito.verify(user).sendRawMessage("message two");
    }

    /**
     * The same message to different users is throttled per user, not globally.
     */
    @Test
    void testNotifyDifferentUsersIndependent() {
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        String message = "a message";
        assertTrue(n.notify(user1, message));
        assertTrue(n.notify(user2, message));
        assertFalse(n.notify(user1, message));
        assertFalse(n.notify(user2, message));
        Mockito.verify(user1).sendRawMessage(message);
        Mockito.verify(user2).sendRawMessage(message);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    void testNotifyUserStringMultisendWaitSend() throws InterruptedException {
        User user = mock(User.class);
        String message = "a message";
        assertTrue(n.notify(user, message));
        for (int i = 0; i< 10; i++) {
            assertFalse(n.notify(user, message));
        }
        Thread.sleep(4100);
        for (int i = 0; i< 10; i++) {
            n.notify(user, message);
        }
        Mockito.verify(user, Mockito.times(2)).sendRawMessage(message);
    }

}
