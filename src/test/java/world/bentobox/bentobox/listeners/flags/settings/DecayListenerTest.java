package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.LeavesDecayEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;

class DecayListenerTest extends CommonTestSetup {

    private DecayListener listener;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.isAllowed(Flags.LEAF_DECAY)).thenReturn(true);

        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        listener = new DecayListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnLeavesDecayAllowed() {
        LeavesDecayEvent e = new LeavesDecayEvent(block);
        listener.onLeavesDecay(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnLeavesDecayNotAllowed() {
        when(island.isAllowed(Flags.LEAF_DECAY)).thenReturn(false);
        LeavesDecayEvent e = new LeavesDecayEvent(block);
        listener.onLeavesDecay(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnLeavesDecayNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        when(island.isAllowed(Flags.LEAF_DECAY)).thenReturn(false);
        LeavesDecayEvent e = new LeavesDecayEvent(block);
        listener.onLeavesDecay(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnLeavesDecayNotOnIslandWorldSettingTrue() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        Flags.LEAF_DECAY.setDefaultSetting(true);
        LeavesDecayEvent e = new LeavesDecayEvent(block);
        listener.onLeavesDecay(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnLeavesDecayNotOnIslandWorldSettingFalse() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        Flags.LEAF_DECAY.setDefaultSetting(false);
        LeavesDecayEvent e = new LeavesDecayEvent(block);
        listener.onLeavesDecay(e);
        assertTrue(e.isCancelled());
    }
}
