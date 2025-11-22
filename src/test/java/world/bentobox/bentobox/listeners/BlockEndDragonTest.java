package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class BlockEndDragonTest extends CommonTestSetup {

    private BlockEndDragon bed;
    @Mock
    private Block block;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // World is the end to start
        when(iwm.isIslandEnd(any())).thenReturn(true);
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        // World
        when(block.getType()).thenReturn(Material.AIR);
        when(block.getY()).thenReturn(255);
        when(block.getX()).thenReturn(0);
        when(block.getZ()).thenReturn(0);
        when(block.getWorld()).thenReturn(world);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(world.getMaxHeight()).thenReturn(256);
        when(world.getEnvironment()).thenReturn(Environment.THE_END);
        when(location.getWorld()).thenReturn(world);
        User.getInstance(mockPlayer);

        // Set flag
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, true);

        // Class
        bed = new BlockEndDragon(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(mockPlayer, world);
        bed.onPlayerChangeWorld(event);
        verify(block).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldNotEnd() {
        when(iwm.isIslandEnd(any())).thenReturn(false);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(mockPlayer, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldBlockSet() {
        when(block.getType()).thenReturn(Material.END_PORTAL);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(mockPlayer, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerChangeWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerChangeWorldNoFlag() {
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, false);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(mockPlayer, world);
        bed.onPlayerChangeWorld(event);
        verify(block, never()).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onPlayerJoinWorld(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinWorld() {
        Component component = mock(Component.class);
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, component);
        bed.onPlayerJoinWorld(event);
        verify(block).setType(eq(Material.END_PORTAL), eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlace() {
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceX() {
        when(block.getX()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceZ() {
        when(block.getZ()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceY() {
        when(block.getY()).thenReturn(23);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceNether() {
        when(world.getEnvironment()).thenReturn(Environment.NETHER);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceNoFlag() {
        Flags.REMOVE_END_EXIT_ISLAND.setSetting(world, false);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnEndBlockPlaceWrongWorld() {
        when(iwm.isEndGenerate(any())).thenReturn(false);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, mockPlayer, false, null);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());

        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(false);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());

        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        bed.onEndBlockPlace(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.BlockEndDragon#onEndBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnEndBlockBreak() {
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        bed.onEndBlockBreak(e);
        assertTrue(e.isCancelled());
    }

}
