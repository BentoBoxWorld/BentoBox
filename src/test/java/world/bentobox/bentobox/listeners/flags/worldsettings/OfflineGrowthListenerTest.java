package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

class OfflineGrowthListenerTest extends CommonTestSetup {

    @Mock
    private Location inside;
    @Mock
    private Block block;
    @Mock
    private BlockState blockState;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Island initialization
        when(island.getOwner()).thenReturn(uuid);
        // Add members
        Builder<UUID> set = new ImmutableSet.Builder<>();
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        when(island.getMemberSet(Mockito.anyInt())).thenReturn(set.build());


        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(inside)).thenReturn(opIsland);

        // Blocks
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);
        when(block.getType()).thenReturn(Material.KELP);
        // Make the inside location return the world (needed for StructureGrowEvent)
        when(inside.getWorld()).thenReturn(world);

        // World Settings
        when(iwm.inWorld(any(World.class))).thenReturn(true);
         WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    void testOnCropGrowDoNothing() {
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, true);
        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    void testOnCropGrowMembersOnline() {
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(mock(Player.class));

        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    void testOnCropGrowMembersOffline() {
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onCropGrow(e);
        // Block growth
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    void testOnCropGrowNonIsland() {
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(inside)).thenReturn(Optional.empty());
        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    void testOnCropGrowNonBentoBoxWorldIsland() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(inside)).thenReturn(Optional.empty());
        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    void testOnSpreadDoNothing() {
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, true);
        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    void testOnSpreadMembersOnline() {
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(mock(Player.class));

        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    void testOnSpreadMembersOffline() {
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onCropGrow(e);
        // Block growth
        assertTrue(e.isCancelled());

        when(block.getType()).thenReturn(Material.BAMBOO);
        orl.onSpread(e);
        // Block growth
        assertTrue(e.isCancelled());

        when(block.getType()).thenReturn(Material.BAMBOO_SAPLING);
        orl.onSpread(e);
        // Block growth
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     * All block spreading on an island with offline members should be blocked,
     * regardless of the material type.
     */
    @Test
    void testOnSpreadMembersOfflineAnyMaterial() {
        when(block.getType()).thenReturn(Material.SPRUCE_LOG);
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are offline
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onSpread(e);
        // Block growth - material filter removed, all spreading is blocked
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    void testOnSpreadNonIsland() {
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(inside)).thenReturn(Optional.empty());
        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    void testOnSpreadNonBentoBoxWorldIsland() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(inside)).thenReturn(Optional.empty());
        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    // ---- StructureGrowEvent tests ----

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * When offline growth is allowed, the event should not be cancelled.
     */
    @Test
    void testOnStructureGrowDoNothing() {
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.TREE, false, null, new ArrayList<>());
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, true);
        orl.onStructureGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * When offline growth is disabled but members are online, growth should be allowed.
     */
    @Test
    void testOnStructureGrowMembersOnline() {
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.TREE, false, null, new ArrayList<>());
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(mock(Player.class));
        orl.onStructureGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * When offline growth is disabled and all members are offline, tree growth should be blocked.
     */
    @Test
    void testOnStructureGrowMembersOffline() {
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.TREE, false, null, new ArrayList<>());
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        orl.onStructureGrow(e);
        // Block growth
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * When the sapling is not on an island, growth should be allowed.
     */
    @Test
    void testOnStructureGrowNonIsland() {
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.TREE, false, null, new ArrayList<>());
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(inside)).thenReturn(Optional.empty());
        orl.onStructureGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * When the event does not occur in a BentoBox world, growth should be allowed.
     */
    @Test
    void testOnStructureGrowNonBentoBoxWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.TREE, false, null, new ArrayList<>());
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        orl.onStructureGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onStructureGrow(StructureGrowEvent)}.
     * Bone-mealed sapling on an island with offline members should also be blocked.
     */
    @Test
    void testOnStructureGrowBoneMealMembersOffline() {
        List<BlockState> blocks = new ArrayList<>();
        StructureGrowEvent e = new StructureGrowEvent(inside, TreeType.BIRCH, true, null, blocks);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        orl.onStructureGrow(e);
        // Block growth even with bone meal
        assertTrue(e.isCancelled());
    }
}
