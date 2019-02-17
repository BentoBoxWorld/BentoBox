/**
 *
 */
package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class })
public class CleanSuperFlatListenerTest {

    private World world;
    private Block block;
    private Chunk chunk;
    private IslandWorldManager iwm;
    private CleanSuperFlatListener l;
    private BukkitScheduler scheduler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        when(plugin.isLoaded()).thenReturn(true);

        // World
        world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // World Settings
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(true);
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        when(iwm.isEndIslands(Mockito.any())).thenReturn(true);


        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta im = mock(ItemMeta.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(im);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

        Flags.CLEAN_SUPER_FLAT.setSetting(world, true);

        chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        block = mock(Block.class);
        when(block.getType()).thenReturn(Material.BEDROCK);
        when(chunk.getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);

        // Fire the ready event
        l = new CleanSuperFlatListener();
        l.onBentoBoxReady(mock(BentoBoxReadyEvent.class));

        // Scheduler
        scheduler = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadNotBedrockNoFlsg() {
        when(block.getType()).thenReturn(Material.AIR);
        Flags.CLEAN_SUPER_FLAT.setSetting(world, false);

        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler, Mockito.never()).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrock() {
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockNoClean() {
        Flags.CLEAN_SUPER_FLAT.setSetting(world, false);

        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler, Mockito.never()).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockNether() {
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(false);
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockEnd() {
        when(world.getEnvironment()).thenReturn(World.Environment.THE_END);
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(false);
        when(iwm.isEndIslands(Mockito.any())).thenReturn(true);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(true);
        when(iwm.isEndIslands(Mockito.any())).thenReturn(false);
        l.onChunkLoad(e);
        Mockito.verify(scheduler).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

}
