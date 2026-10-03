package world.bentobox.bentobox.api.commands.admin.deaths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent;
import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent.Action;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests that the admin deaths commands fire {@link PlayerDeathsChangedEvent}.
 */
class AdminDeathsCommandsTest extends CommonTestSetup {

    private static final String TARGET = "target";

    @Mock
    private AdminDeathsCommand parent;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private UUID targetUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);
        User.setPlugin(plugin);
        when(parent.getWorld()).thenReturn(world);
        when(plugin.getPlayers()).thenReturn(pm);
        targetUUID = UUID.randomUUID();
        when(pm.getUUID(TARGET)).thenReturn(targetUUID);
        mockedUtil.when(() -> Util.getUUID(anyString())).thenCallRealMethod();
        when(pm.getDeaths(world, targetUUID)).thenReturn(5);
    }

    private PlayerDeathsChangedEvent captureEvent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        return (PlayerDeathsChangedEvent) captor.getValue();
    }

    private void assertEvent(Action action, int amount, int oldDeaths, int newDeaths) {
        PlayerDeathsChangedEvent e = captureEvent();
        assertSame(world, e.getWorld());
        assertEquals(targetUUID, e.getPlayerUUID());
        assertEquals(action, e.getAction());
        assertEquals(amount, e.getAmount());
        assertEquals(oldDeaths, e.getOldDeaths());
        assertEquals(newDeaths, e.getNewDeaths());
    }

    @Test
    void testSetFiresEvent() {
        assertTrue(new AdminDeathsSetCommand(parent).execute(user, "", List.of(TARGET, "2")));
        verify(pm).setDeaths(world, targetUUID, 2);
        assertEvent(Action.SET, 2, 5, 2);
    }

    @Test
    void testAddFiresEvent() {
        // After the set, the players manager reports the new total
        when(pm.getDeaths(world, targetUUID)).thenReturn(5, 8);
        assertTrue(new AdminDeathsAddCommand(parent).execute(user, "", List.of(TARGET, "3")));
        verify(pm).setDeaths(world, targetUUID, 8);
        assertEvent(Action.ADD, 3, 5, 8);
    }

    @Test
    void testRemoveFiresEventWithRequestedAmount() {
        assertTrue(new AdminDeathsRemoveCommand(parent).execute(user, "", List.of(TARGET, "7")));
        verify(pm).setDeaths(world, targetUUID, 0);
        // The requested amount is reported even though the count is clamped at zero
        assertEvent(Action.REMOVE, 7, 5, 0);
    }

    @Test
    void testResetFiresEvent() {
        assertTrue(new AdminDeathsResetCommand(parent).execute(user, "", List.of(TARGET)));
        verify(pm).setDeaths(world, targetUUID, 0);
        assertEvent(Action.RESET, 0, 5, 0);
    }

    @Test
    void testNoEventOnBadInput() {
        assertFalse(new AdminDeathsSetCommand(parent).execute(user, "", List.of(TARGET, "-1")));
        assertFalse(new AdminDeathsRemoveCommand(parent).execute(user, "", List.of("unknown", "1")));
        verify(pim, never()).callEvent(any());
    }
}
