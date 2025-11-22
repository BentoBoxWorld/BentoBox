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
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 *
 */
public class SculkSensorListenerTest extends CommonTestSetup {

    private SculkSensorListener ssl;
    @Mock
    private Block block;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        // Block
        when(block.getType()).thenReturn(Material.SCULK_SENSOR);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);

        // User
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getLocation()).thenReturn(location);
        User.getInstance(mockPlayer);

        ssl = new SculkSensorListener();
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorAllowed() {
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotAllowedCalibrated() {
        when(block.getType()).thenReturn(Material.CALIBRATED_SCULK_SENSOR);
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorAllowedCalibrated() {
        when(block.getType()).thenReturn(Material.CALIBRATED_SCULK_SENSOR);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotInWorldCalibrated() {
        when(block.getType()).thenReturn(Material.CALIBRATED_SCULK_SENSOR);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotAllowedNotSculk() {
        when(block.getType()).thenReturn(Material.SHULKER_BOX);
        when(island.isAllowed(any(), any())).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorAllowedNotSculk() {
        when(block.getType()).thenReturn(Material.SHULKER_BOX);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener#onSculkSensor(org.bukkit.event.block.BlockReceiveGameEvent)}.
     */
    @Test
    public void testOnSculkSensorNotInWorldNotSculk() {
        when(block.getType()).thenReturn(Material.SHULKER_BOX);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        BlockReceiveGameEvent e = new BlockReceiveGameEvent(GameEvent.BLOCK_ACTIVATE, block, mockPlayer);
        ssl.onSculkSensor(e);
        assertFalse(e.isCancelled());
    }

}
