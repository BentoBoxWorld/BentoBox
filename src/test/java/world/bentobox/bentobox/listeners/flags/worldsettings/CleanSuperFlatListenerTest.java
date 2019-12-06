package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.MyBiomeGrid;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class })
public class CleanSuperFlatListenerTest {

    @Mock
    private World world;
    @Mock
    private Block block;
    @Mock
    private Chunk chunk;
    @Mock
    private IslandWorldManager iwm;
    private CleanSuperFlatListener l;
    @Mock
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
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getName()).thenReturn("world");

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // World Settings
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.isUseOwnGenerator(any())).thenReturn(false);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());


        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta im = mock(ItemMeta.class);
        when(itemF.getItemMeta(any())).thenReturn(im);
        when(Bukkit.getItemFactory()).thenReturn(itemF);
        // Default is that flag is active
        Flags.CLEAN_SUPER_FLAT.setSetting(world, true);
        // Default is that chunk has bedrock
        when(chunk.getWorld()).thenReturn(world);
        // Super flat!
        when(block.getType()).thenReturn(Material.BEDROCK, Material.DIRT, Material.DIRT, Material.GRASS_BLOCK);
        when(chunk.getBlock(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);

        // Fire the ready event
        l = new CleanSuperFlatListener();
        l.onBentoBoxReady(mock(BentoBoxReadyEvent.class));

        // Scheduler
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // Addons Manager
        AddonsManager am = mock(AddonsManager.class);
        @Nullable
        ChunkGenerator cg = mock(ChunkGenerator.class);
        ChunkData cd = mock(ChunkData.class);
        when(cg.generateChunkData(any(World.class), any(Random.class), anyInt(), anyInt(), any(MyBiomeGrid.class))).thenReturn(cd);
        BlockData bd = mock(BlockData.class);
        when(cd.getBlockData(anyInt(), anyInt(), anyInt())).thenReturn(bd);

        when(plugin.getAddonsManager()).thenReturn(am);
        when(am.getDefaultWorldGenerator(anyString(), anyString())).thenReturn(cg);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
        verify(scheduler, never()).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrock() {
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockNoClean() {
        Flags.CLEAN_SUPER_FLAT.setSetting(world, false);

        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        verify(scheduler, never()).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockNether() {
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isNetherGenerate(any())).thenReturn(false);
        when(iwm.isNetherIslands(any())).thenReturn(true);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(false);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

    /**
     * Test method for {@link CleanSuperFlatListener#onChunkLoad(org.bukkit.event.world.ChunkLoadEvent)}.
     */
    @Test
    public void testOnChunkLoadBedrockEnd() {
        when(world.getEnvironment()).thenReturn(World.Environment.THE_END);
        ChunkLoadEvent e = new ChunkLoadEvent(chunk, false);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isEndGenerate(any())).thenReturn(false);
        when(iwm.isEndIslands(any())).thenReturn(true);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(false);
        l.onChunkLoad(e);
        verify(scheduler).runTaskTimer(any(), any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));
    }

}
