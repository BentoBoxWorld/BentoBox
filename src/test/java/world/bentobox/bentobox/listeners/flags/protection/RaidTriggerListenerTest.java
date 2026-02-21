package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.event.raid.RaidTriggerEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Tests for {@link RaidTriggerListener}.
 */
public class RaidTriggerListenerTest extends CommonTestSetup {

    private RaidTriggerListener listener;
    private RaidTriggerEvent event;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the flag to ensure it's loaded before use in argument matchers
        Flags.RAID_TRIGGER.setDefaultSetting(false);

        // Mock the RaidTriggerEvent
        event = mock(RaidTriggerEvent.class);
        when(event.getPlayer()).thenReturn(mockPlayer);
        when(event.getWorld()).thenReturn(world);

        listener = new RaidTriggerListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that a player without the required rank cannot trigger a raid.
     */
    @Test
    public void testOnRaidTriggerNotAllowed() {
        when(island.isAllowed(any(User.class), eq(Flags.RAID_TRIGGER))).thenReturn(false);
        listener.onRaidTrigger(event);
        verify(notifier).notify(any(), eq("protection.protected"));
        verify(event).setCancelled(true);
    }

    /**
     * Test that a player with the required rank can trigger a raid.
     */
    @Test
    public void testOnRaidTriggerAllowed() {
        when(island.isAllowed(any(User.class), eq(Flags.RAID_TRIGGER))).thenReturn(true);
        listener.onRaidTrigger(event);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        verify(event, never()).setCancelled(true);
    }
}
