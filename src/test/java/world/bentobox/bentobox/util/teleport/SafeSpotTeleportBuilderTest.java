package world.bentobox.bentobox.util.teleport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SafeSpotTeleport.Builder.class)
public class SafeSpotTeleportBuilderTest {

    @Mock
    private SafeSpotTeleport sst;
    @Mock
    private BentoBox plugin;
    @Mock
    private Player player;
    @Mock
    private Location loc;

    @InjectMocks
    private SafeSpotTeleport.Builder sstb;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(SafeSpotTeleport.class).withAnyArguments().thenReturn(sst);
        // Users
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");
        // Addon
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());
        when(plugin.getIWM()).thenReturn(iwm);

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testBuilder() {
        sstb = new SafeSpotTeleport.Builder(plugin);
        // Should fail because no data
        assertNull(sstb.build());
    }

    @Test
    public void testEntity() throws Exception {
        // Start builder
        sstb = new SafeSpotTeleport.Builder(plugin);
        // Add entity
        sstb.entity(player);
        // Test for error
        assertNull(sstb.build());
        // Add location
        sstb.location(loc);
        // Build - expect success
        SafeSpotTeleport result = sstb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testIsland() {
        // Start builder
        SafeSpotTeleport.Builder sstb = new SafeSpotTeleport.Builder(plugin);
        // Add entity
        sstb.entity(player);
        // Add island
        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(loc);
        sstb.island(island);
        // Build - expect success
        SafeSpotTeleport result = sstb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testHomeNumber() {
        // Start builder
        SafeSpotTeleport.Builder sstb = new SafeSpotTeleport.Builder(plugin);
        // Add entity
        sstb.entity(player);
        // Add location
        sstb.location(loc);
        // Add home
        sstb.homeNumber(10);
        // Build - expect success
        SafeSpotTeleport result = sstb.build();
        assertEquals(sst, result);

    }

    @Test
    public void testPortal() {
        // Start builder
        SafeSpotTeleport.Builder sstb = new SafeSpotTeleport.Builder(plugin);
        // Add entity
        sstb.entity(player);
        // Add location
        sstb.location(loc);
        // Portal
        sstb.portal();
        // Build - expect success
        SafeSpotTeleport result = sstb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testFailureMessage() {
        // Start builder
        SafeSpotTeleport.Builder sstb = new SafeSpotTeleport.Builder(plugin);
        // Add entity
        sstb.entity(player);
        // Add location
        sstb.location(loc);
        // Add failure
        sstb.failureMessage("testing 123");
        // Build - expect success
        SafeSpotTeleport result = sstb.build();
        assertEquals(sst, result);
    }
}
