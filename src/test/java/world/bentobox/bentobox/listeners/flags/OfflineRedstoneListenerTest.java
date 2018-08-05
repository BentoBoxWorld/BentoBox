package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertEquals;
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
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class OfflineRedstoneListenerTest {

    private World world;
    private IslandsManager im;
    private Location inside;
    private Block block;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        world = mock(World.class);
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
        when(island.getMemberSet()).thenReturn(set.build());


        im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

        inside = mock(Location.class);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);

        // Blocks
        block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // World Settings
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        PowerMockito.mockStatic(Bukkit.class);
    }

    @Test
    public void testOnBlockRedstoneDoNothing() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        Flags.OFFLINE_REDSTONE.setSetting(world, true);
        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    @Test
    public void testOnBlockRedstoneMembersOnline() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(Mockito.any(UUID.class))).thenReturn(mock(Player.class));

        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    @Test
    public void testOnBlockRedstoneMembersOffline() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(Mockito.any(UUID.class))).thenReturn(null);

        orl.onBlockRedstone(e);
        // Current will be 0
        assertEquals(0, e.getNewCurrent());
    }

    @Test
    public void testOnBlockRedstoneNonIsland() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(Optional.empty());
        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

}
