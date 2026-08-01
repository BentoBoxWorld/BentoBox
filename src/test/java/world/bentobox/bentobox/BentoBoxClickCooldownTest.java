package world.bentobox.bentobox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.ExpiringMap;

/**
 * Tests the panel click cooldown, {@link BentoBox#onTimeout(User, Panel, boolean)}.
 *
 * @author tastybento
 */
class BentoBoxClickCooldownTest extends CommonTestSetup {

    private User user;
    private Panel panel;
    private int tick;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = User.getInstance(mockPlayer);
        panel = mock(Panel.class);
        when(panel.getName()).thenReturn("settings");

        // The cooldown map is normally created in onEnable, which never runs for a mocked plugin
        Field field = BentoBox.class.getDeclaredField("lastClick");
        field.setAccessible(true);
        field.set(plugin, new ExpiringMap<>(1, TimeUnit.MINUTES));

        doCallRealMethod().when(plugin).onTimeout(any(User.class), any(Panel.class), anyBoolean());
        doCallRealMethod().when(plugin).onTimeout(any(User.class), any(Panel.class));

        // Drive the server tick by hand
        tick = 100;
        mockedBukkit.when(Bukkit::getCurrentTick).thenAnswer(invocation -> tick);
    }

    /**
     * The first click of a window is always allowed.
     */
    @Test
    void testFirstClickAllowed() {
        assertFalse(plugin.onTimeout(user, panel, true));
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * A second click in the same tick is a duplicate packet, not spam, so it is dropped without
     * telling the player to slow down. See #3049.
     */
    @Test
    void testSameTickDuplicateIsSilentlyDropped() {
        assertFalse(plugin.onTimeout(user, panel, true));
        assertTrue(plugin.onTimeout(user, panel, true));
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Geyser expands one Bedrock tap into several Java click packets, and the packet that lands on
     * the button is not necessarily the first one. The click that can do work must survive even if
     * a no-op click opened the window in the same tick. See #3049.
     */
    @Test
    void testActionableClickSurvivesEarlierSameTickNoOpClick() {
        // A click that lands on a filler icon or the player's own inventory opens the window
        assertFalse(plugin.onTimeout(user, panel, false));
        // The click that actually presses the button, same tick, still gets through
        assertFalse(plugin.onTimeout(user, panel, true));
        // But only once - any further clicks in that tick are dropped, silently
        assertTrue(plugin.onTimeout(user, panel, true));
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Clicks in a later tick but still inside the cooldown are genuine spam and are rejected with
     * the "slow down" notice, which is only sent once per window.
     */
    @Test
    void testLaterTickInsideCooldownIsRejectedAndNotifiedOnce() {
        assertFalse(plugin.onTimeout(user, panel, true));
        tick++;
        assertTrue(plugin.onTimeout(user, panel, true));
        tick++;
        assertTrue(plugin.onTimeout(user, panel, true));
        // The notice is translated and sent at most once per window
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notifier, times(1)).notify(any(), captor.capture());
        assertTrue(captor.getValue().contains("slow-down"), captor.getValue());
    }

    /**
     * The cooldown is per panel, so a click on a different panel is not blocked.
     */
    @Test
    void testCooldownIsPerPanel() {
        Panel other = mock(Panel.class);
        when(other.getName()).thenReturn("other");
        assertFalse(plugin.onTimeout(user, panel, true));
        tick++;
        assertFalse(plugin.onTimeout(user, other, true));
    }

    /**
     * The two argument version treats the click as one that can do work.
     */
    @Test
    void testTwoArgOverloadIsActionable() {
        assertFalse(plugin.onTimeout(user, panel, false));
        // Would only be allowed if the click counts as actionable
        assertFalse(plugin.onTimeout(user, panel));
    }
}
