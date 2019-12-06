package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * Tests {@link TreesGrowingOutsideRangeListener}.
 * @author Poslovitch
 * @since 1.3.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class})
public class TreesGrowingOutsideRangeListenerTest {

    /* IslandWorldManager */
    @Mock
    private IslandWorldManager iwm;

    /* Event */
    private StructureGrowEvent event;

    /* Block */
    @Mock
    private Block sapling;
    private List<BlockState> blockStates;

    /* World */
    @Mock
    private World world;

    /* Islands */
    @Mock
    private IslandsManager islandsManager;

    @Mock
    private Island island;

    @Mock
    private BlockState firstBlock;
    @Mock
    private BlockState lastBlock;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        /* Blocks */
        when(sapling.getType()).thenReturn(Material.OAK_SAPLING);
        when(sapling.getLocation()).thenReturn(new Location(world, 2, 0, 2));

        blockStates = new ArrayList<>();
        populateBlockStatesList();

        /* Event */
        event = new StructureGrowEvent(sapling.getLocation(), TreeType.TREE, false, null, blockStates);

        /* Island World Manager */
        when(plugin.getIWM()).thenReturn(iwm);


        // WorldSettings and World Flags
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // By default everything is in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        /* Flags */
        // By default, it is not allowed
        Flags.TREES_GROWING_OUTSIDE_RANGE.setSetting(world, false);

        /* Islands */
        when(plugin.getIslands()).thenReturn(islandsManager);
        // By default, there should be an island.
        when(islandsManager.getProtectedIslandAt(any())).thenReturn(Optional.of(island));
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Populates {@link TreesGrowingOutsideRangeListenerTest#blockStates} with a tree schema.
     */
    private void populateBlockStatesList() {
        //when(firstBlock.getLocation()).thenReturn(new Location(world, 2, 0, 2));
        blockStates.add(firstBlock);
        // Tree logs
        for (int i = 0; i < 3; i++) {
            BlockState logState = mock(BlockState.class);
            when(logState.getType()).thenReturn(Material.OAK_LOG);
            when(logState.getLocation()).thenReturn(new Location(world, 2, i, 2));
            blockStates.add(logState);
        }

        // Basic leaves pattern
        for (int x = 0; x < 5 ; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 5; z++) {
                    if (x != 2 && y >= 3 && z != 2) {
                        BlockState leafState = mock(BlockState.class);
                        when(leafState.getType()).thenReturn(Material.OAK_LEAVES);
                        when(leafState.getLocation()).thenReturn(new Location(world, x, y, z));
                        blockStates.add(leafState);
                    }
                }
            }
        }
        //when(lastBlock.getLocation()).thenReturn(new Location(world, 2, 0, 2));
        blockStates.add(lastBlock);
    }

    /**
     * Asserts that no interaction is done to the event when it does not happen in the world.
     */
    @Test
    public void testNotInWorld() {
        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);

        // Run
        new TreesGrowingOutsideRangeListener().onTreeGrow(event);
        assertEquals(blockStates, event.getBlocks());
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that no interaction is done to the event when {@link Flags#TREES_GROWING_OUTSIDE_RANGE} is allowed.
     */
    @Test
    public void testFlagIsAllowed() {
        // Allowed
        Flags.TREES_GROWING_OUTSIDE_RANGE.setSetting(world, true);

        // Run
        new TreesGrowingOutsideRangeListener().onTreeGrow(event);
        assertEquals(blockStates, event.getBlocks());
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that the event is cancelled and that there is no interaction with the blocks list when the sapling is outside an island.
     */
    @Test
    public void testSaplingOutsideIsland() {
        // No protected island at the sapling's location
        when(islandsManager.getProtectedIslandAt(sapling.getLocation())).thenReturn(Optional.empty());

        // Run
        new TreesGrowingOutsideRangeListener().onTreeGrow(event);
        assertEquals(blockStates, event.getBlocks());
        assertTrue(event.isCancelled());
    }

    /**
     * Asserts that no interaction is done to the event when everything's inside an island.
     */
    @Test
    public void testTreeFullyInsideIsland() {
        // Run
        new TreesGrowingOutsideRangeListener().onTreeGrow(event);
        assertEquals(blockStates, event.getBlocks());
        assertFalse(event.isCancelled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTreePartiallyOutsideIsland() {
        // Only the first few blocks are inside the island
        when(islandsManager.getProtectedIslandAt(any())).thenReturn(Optional.of(island),
                Optional.of(island),
                Optional.of(island),
                Optional.empty());
        // Run
        new TreesGrowingOutsideRangeListener().onTreeGrow(event);
        assertFalse(event.isCancelled());
        verify(firstBlock, Mockito.never()).setType(Material.AIR);
        verify(lastBlock).setType(Material.AIR);
    }
}
