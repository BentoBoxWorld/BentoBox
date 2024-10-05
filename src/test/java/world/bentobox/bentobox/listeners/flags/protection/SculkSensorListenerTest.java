package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@Ignore("Has mocking issues with GameEvent")
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class SculkSensorListenerTest extends AbstractCommonSetup {

    private SculkSensorListener ssl;
    @Mock
    private Block block;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
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
