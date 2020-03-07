package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy.Result;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
public class DefaultNewIslandLocationStrategyTest {

    private DefaultNewIslandLocationStrategy dnils;

    @Mock
    private BentoBox plugin;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandDeletionManager idm;
    @Mock
    private Block block;
    @Mock
    private Block adjBlock;

    private int count;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(100D);
        when(location.getZ()).thenReturn(-100D);
        when(location.getBlock()).thenReturn(block);
        // Block
        when(block.getRelative(any())).thenReturn(adjBlock);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        when(adjBlock.getType()).thenReturn(Material.AIR);
        when(adjBlock.isEmpty()).thenReturn(true);
        // Islands manager
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(eq(world))).thenReturn(50);
        when(iwm.getIslandHeight(eq(world))).thenReturn(120);
        when(iwm.getIslandXOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandStartX(eq(world))).thenReturn(1000);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(11000);
        // Island deletion manager
        when(plugin.getIslandDeletionManager()).thenReturn(idm);
        when(idm.inDeletion(any())).thenReturn(false);
        // Util
        PowerMockito.mockStatic(Util.class);
        // Return back what the argument was, i.e., no change
        when(Util.getClosestIsland(any())).thenAnswer((Answer<Location>) invocation -> invocation.getArgument(0, Location.class));
        // Default is that chunks have been generated
        when(Util.isChunkGenerated(any())).thenReturn(true);
        // Last island location
        when(im.getLast(eq(world))).thenReturn(location);
        // Class under test
        dnils = new DefaultNewIslandLocationStrategy();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#getNextLocation(org.bukkit.World)}.
     */
    @Test
    public void testGetNextLocationSuccess() {
        assertEquals(location,dnils.getNextLocation(world));
        verify(im).setLast(location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#getNextLocation(org.bukkit.World)}.
     */
    @Test
    public void testGetNextLocationFailBlocks() {
        when(adjBlock.getType()).thenReturn(Material.STONE);
        when(adjBlock.isEmpty()).thenReturn(false);
        assertNull(dnils.getNextLocation(world));
        verify(plugin).logError("Could not find a free spot for islands! Is this world empty?");
        verify(plugin).logError("Blocks around center locations: 20 max 20");
        verify(plugin).logError("Known islands: 0 max unlimited.");
        verify(im, never()).setLast(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#getNextLocation(org.bukkit.World)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetNextLocationSuccessSomeIslands() {
        Optional<Island> opIsland = Optional.of(new Island());
        Optional<Island> emptyIsland = Optional.empty();
        when(im.getIslandAt(any())).thenReturn(opIsland, opIsland, opIsland, opIsland, emptyIsland);
        assertEquals(location,dnils.getNextLocation(world));
        verify(im).setLast(location);
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#getNextLocation(org.bukkit.World)}.
     */
    @Test
    public void testGetNextLocationSuccessSomeIslands10() {
        Optional<Island> opIsland = Optional.of(new Island());
        Optional<Island> emptyIsland = Optional.empty();
        count = 0;
        //long time = System.currentTimeMillis();
        when(im.getIslandAt(any())).thenAnswer(i -> count++ > 10 ? emptyIsland :opIsland);
        assertEquals(location,dnils.getNextLocation(world));
        //System.out.println(System.currentTimeMillis() - time);
        verify(im).setLast(location);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandIslandFound() {
        Optional<Island> opIsland = Optional.of(new Island());
        when(im.getIslandAt(any())).thenReturn(opIsland);
        assertEquals(Result.ISLAND_FOUND, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandIslandInDeletion() {
        when(idm.inDeletion(any())).thenReturn(true);
        assertEquals(Result.ISLAND_FOUND, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandChunkNotGenerated() {
        when(Util.isChunkGenerated(any())).thenReturn(false);
        assertEquals(Result.FREE, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandUseOwnGenerator() {
        when(iwm.isUseOwnGenerator(eq(world))).thenReturn(true);
        assertEquals(Result.FREE, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandFreeAirBlocks() {
        assertEquals(Result.FREE, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandFreeWaterBlocks() {
        when(adjBlock.getType()).thenReturn(Material.WATER);
        when(adjBlock.isEmpty()).thenReturn(false);
        assertEquals(Result.FREE, dnils.isIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy#isIsland(org.bukkit.Location)}.
     */
    @Test
    public void testIsIslandBlocksInArea() {
        when(adjBlock.getType()).thenReturn(Material.STONE);
        when(adjBlock.isEmpty()).thenReturn(false);
        assertEquals(Result.BLOCKS_IN_AREA, dnils.isIsland(location));
    }

}
