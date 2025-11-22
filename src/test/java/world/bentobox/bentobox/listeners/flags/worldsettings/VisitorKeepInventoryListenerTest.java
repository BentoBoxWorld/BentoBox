package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class VisitorKeepInventoryListenerTest extends CommonTestSetup {

    // Class under test
    private VisitorKeepInventoryListener l;
    private PlayerDeathEvent e;

    @SuppressWarnings("deprecation")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // User
        User.setPlugin(plugin);
        
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getName()).thenReturn("tastybento");
        when(mockPlayer.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.toVector()).thenReturn(new Vector(1,2,3));
        // Turn on why for player
        when(mockPlayer.getMetadata(eq("bskyblock_world_why_debug"))).thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, true)));
        when(mockPlayer.getMetadata(eq("bskyblock_world_why_debug_issuer"))).thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, uuid.toString())));
        User.getInstance(mockPlayer);

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

        // Visitor
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of());
        // By default, there should be an island.
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));

        // Util
        mockedUtil.when(()-> Util.getWorld(any())).thenReturn(world);

        // Default death event
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.ACACIA_BOAT));
        e = getPlayerDeathEvent(mockPlayer, drops, 100, 0, 0, 0, "Death message");
        // Make new
        l = new VisitorKeepInventoryListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
        checkSpigotMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        checkSpigotMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_NOT_ALLOWED_IN_WORLD");
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
        checkSpigotMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        checkSpigotMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_ALLOWED_IN_WORLD");
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
        checkSpigotMessage("Why: PlayerDeathEvent in world bskyblock_world at 1,2,3");
        checkSpigotMessage("Why: tastybento VISITOR_KEEP_INVENTORY - SETTING_NOT_ALLOWED_IN_WORLD");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener#onVisitorDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnVisitorDeathTrueFlagNoIsland() {
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        Flags.VISITOR_KEEP_INVENTORY.setSetting(world, true);
        l.onVisitorDeath(e);
        assertFalse(e.getKeepInventory());
        assertFalse(e.getKeepLevel());
        assertFalse(e.getDrops().isEmpty());
        assertEquals(100, e.getDroppedExp());
        // Why
        verify(mockPlayer, never()).sendMessage(anyString());
    }

}
