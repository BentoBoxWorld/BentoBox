package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

class SculkShriekerListenerTest extends CommonTestSetup {

    private SculkShriekerListener listener;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        when(block.getType()).thenReturn(Material.SCULK_SHRIEKER);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);

        listener = new SculkShriekerListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnSculkShriekerAllowed() {
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        listener.onSculkShrieker(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSculkShriekerNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        listener.onSculkShrieker(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnSculkShriekerNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        listener.onSculkShrieker(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSculkShriekerNotSculkShrieker() {
        when(block.getType()).thenReturn(Material.STONE);
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        listener.onSculkShrieker(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnSculkShriekerNullEntity() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, null);
        listener.onSculkShrieker(e);
        assertFalse(e.isCancelled());
    }
}
