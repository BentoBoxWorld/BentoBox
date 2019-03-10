package world.bentobox.bentobox.api.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author tastybento
 *
 */
public class NotifierTest {

    private Notifier n;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        n = new Notifier();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testNotifyUserString() {
        User user = mock(User.class);
        String message = "a message";
        assertTrue(n.notify(user, message));
        Mockito.verify(user).sendRawMessage(message);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testNotifyUserStringMultisend() {
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
     * Test method for {@link world.bentobox.bentobox.api.user.Notifier#notify(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws InterruptedException
     */
    @Test
    public void testNotifyUserStringMultisendWaitSend() throws InterruptedException {
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
