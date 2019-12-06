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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockGrowEvent;
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
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

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

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // World Settings
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        PowerMockito.mockStatic(Bukkit.class);
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

}
