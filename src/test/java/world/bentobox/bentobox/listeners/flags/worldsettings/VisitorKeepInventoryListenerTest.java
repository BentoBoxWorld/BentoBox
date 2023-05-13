package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class})
public class VisitorKeepInventoryListenerTest {

    // Class under test
    private VisitorKeepInventoryListener l;
    @Mock
    private Player player;
    /* IslandWorldManager */
    @Mock
    private IslandWorldManager iwm;
    /* World */
    @Mock
    private World world;

    /* Islands */
    @Mock
    private IslandsManager islandsManager;

    @Mock
    private Island island;
    private PlayerDeathEvent e;
    @Mock
    private Location location;


    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        /* Island World Manager */
        when(plugin.getIWM()).thenReturn(iwm);

        // User
        User.setPlugin(plugin);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("tastybento");
        when(player.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.toVector()).thenReturn(new Vector(1,2,3));
        // Turn on why for player
        when(player.getMetadata(eq("bskyblock_world_why_debug"))).thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, true)));
        when(player.getMetadata(eq("bskyblock_world_why_debug_issuer"))).thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, uuid.toString())));
        User.getInstance(player);

        // WorldSettings and World Flags
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // World
        when(world.getName()).thenReturn("bskyblock_world");

        // By default everything is in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Default not set
        Flags.VISITOR_KEEP_INVENTORY.setSetting(world, false);

        /* Islands */
        when(plugin.getIslands()).thenReturn(islandsManager);
        // Visitor
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of());
        // By default, there should be an island.
        when(islandsManager.getProtectedIslandAt(any())).thenReturn(Optional.of(island));

        // Util
        PowerMockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        when(Util.getWorld(any())).thenReturn(world);

        // Default death event
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.ACACIA_BOAT));
        e = new PlayerDeathEvent(player, drops, 100, 0, 0, 0, "Death message");
        // Make new
        l = new VisitorKeepInventoryListener();
    }

    /**
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeath() {
        l.onVisitorDeath(e);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeathFalseFlag() {
        l.onVisitorDeath(e);
        assertFalse(e.getKeepInventory());
        assertFalse(e.getKeepLevel());
        assertFalse(e.getDrops().isEmpty());
        assertEquals(100, e.getDroppedExp());
        // Why
        verify(player).sendMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        verify(player).sendMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_NOT_ALLOWED_IN_WORLD");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeathTrueFlag() {
        Flags.VISITOR_KEEP_INVENTORY.setSetting(world, true);
        l.onVisitorDeath(e);
        assertTrue(e.getKeepInventory());
        assertTrue(e.getKeepLevel());
        assertTrue(e.getDrops().isEmpty());
        assertEquals(0, e.getDroppedExp());
        // Why
        verify(player).sendMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        verify(player).sendMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_ALLOWED_IN_WORLD");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeathNotInWorld() {
        when(iwm.inWorld(eq(world))).thenReturn(false);
        Flags.VISITOR_KEEP_INVENTORY.setSetting(world, true);
        l.onVisitorDeath(e);
        assertFalse(e.getKeepInventory());
        assertFalse(e.getKeepLevel());
        assertFalse(e.getDrops().isEmpty());
        assertEquals(100, e.getDroppedExp());
        // Why
        verify(player).sendMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        verify(player).sendMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_NOT_ALLOWED_IN_WORLD");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeathTrueFlagNoIsland() {
        when(islandsManager.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        Flags.VISITOR_KEEP_INVENTORY.setSetting(world, true);
        l.onVisitorDeath(e);
        assertFalse(e.getKeepInventory());
        assertFalse(e.getKeepLevel());
        assertFalse(e.getDrops().isEmpty());
        assertEquals(100, e.getDroppedExp());
        // Why
        verify(player, never()).sendMessage(anyString());
    }

}
