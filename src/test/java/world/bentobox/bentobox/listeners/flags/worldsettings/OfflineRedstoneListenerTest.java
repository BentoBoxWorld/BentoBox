package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
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
public class OfflineRedstoneListenerTest {

    private static final String[] NAMES = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};

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
    private Island island;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Owner
        UUID uuid = UUID.randomUUID();

        // Island initialization
        when(island.getOwner()).thenReturn(uuid);
        // Add members
        Builder<UUID> set = new ImmutableSet.Builder<>();
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        set.add(UUID.randomUUID());
        when(island.getMemberSet(Mockito.anyInt())).thenReturn(set.build());


        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);

        // Blocks
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(inside);

        // Util
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
        // Online players
        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < NAMES.length; j++) {
            Player p1 = mock(Player.class);
            UUID u = UUID.randomUUID();
            when(p1.getUniqueId()).thenReturn(u);
            when(p1.getName()).thenReturn(NAMES[j]);
            // All ops
            when(p1.isOp()).thenReturn(true);
            onlinePlayers.add(p1);
        }        
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
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

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneMembersOnline() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(mock(Player.class));

        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneMembersOffline() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are offline
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onBlockRedstone(e);
        // Current will be 0
        assertEquals(0, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneMembersOfflineOpsOnlineNotOnIsland() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are offline
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onBlockRedstone(e);
        // Current will be 0
        assertEquals(0, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneMembersOfflineOpsOnlineOnIsland() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are offline
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        // On island
        when(island.onIsland(any())).thenReturn(true);

        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneMembersOfflineSpawn() {
        when(island.isSpawn()).thenReturn(true);
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        // Offline redstone not allowed
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        // Members are online
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneNonIsland() {
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

    /**
     * Test method for {@link OfflineRedstoneListener#onBlockRedstone(BlockRedstoneEvent)}.
     */
    @Test
    public void testOnBlockRedstoneNonBentoBoxWorldIsland() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        // Make an event to give some current to block
        BlockRedstoneEvent e = new BlockRedstoneEvent(block, 0, 10);
        OfflineRedstoneListener orl = new OfflineRedstoneListener();
        Flags.OFFLINE_REDSTONE.setSetting(world, false);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(Optional.empty());
        orl.onBlockRedstone(e);
        // Current remains 10
        assertEquals(10, e.getNewCurrent());
    }

}
