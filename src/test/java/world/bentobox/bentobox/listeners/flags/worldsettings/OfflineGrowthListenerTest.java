package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class OfflineGrowthListenerTest {

    @Mock
    private World world;
    @Mock
    private IslandsManager im;
    @Mock
    private Location inside;
    @Mock
    private Block block;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private BlockState blockState;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Owner
        UUID uuid = UUID.randomUUID();

        // Island initialization
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        // Add members
        Builder<UUID> set = new ImmutableSet.Builder<>();
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        when(island.getMemberSet(Mockito.anyInt())).thenReturn(set.build());


        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);

        // Blocks
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);
        when(block.getType()).thenReturn(Material.KELP);

        // World Settings
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    public void testOnCropGrowDoNothing() {
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
    public void testOnCropGrowMembersOnline() {
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
    public void testOnCropGrowMembersOffline() {
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
    public void testOnCropGrowNonIsland() {
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#OnCropGrow(BlockGrowEvent)}.
     */
    @Test
    public void testOnCropGrowNonBentoBoxWorldIsland() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        // Make an event to give some current to block
        BlockGrowEvent e = new BlockGrowEvent(block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onCropGrow(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    public void testOnSpreadDoNothing() {
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
    public void testOnSpreadMembersOnline() {
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
    public void testOnSpreadMembersOffline() {
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
     */
    @Test
    public void testOnSpreadMembersOfflineTree() {
        when(block.getType()).thenReturn(Material.SPRUCE_LOG);
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        // Offline Growth not allowed
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onSpread(e);
        // Do not block growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    public void testOnSpreadNonIsland() {
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link OfflineGrowthListener#onSpread(BlockSpreadEvent)}.
     */
    @Test
    public void testOnSpreadNonBentoBoxWorldIsland() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        // Make an event to give some current to block
        BlockSpreadEvent e = new BlockSpreadEvent(block, block, blockState);
        OfflineGrowthListener orl = new OfflineGrowthListener();
        Flags.OFFLINE_GROWTH.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onSpread(e);
        // Allow growth
        assertFalse(e.isCancelled());
    }
}
