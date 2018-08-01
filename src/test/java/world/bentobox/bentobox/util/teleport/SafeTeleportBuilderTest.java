package world.bentobox.bentobox.util.teleport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.LocalesManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SafeTeleportBuilder.class)
public class SafeTeleportBuilderTest {

    @Mock
    private SafeSpotTeleport sst;
    @Mock
    private BentoBox plugin;
    @Mock
    private Player player;
    @Mock
    private Location loc;

    @InjectMocks
    private SafeTeleportBuilder stb;

    @Before
    public void setUp() throws Exception {
        PowerMockito.whenNew(SafeSpotTeleport.class).withAnyArguments().thenReturn(sst);
        // Users
        User.setPlugin(plugin);
        // Locales - final        
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");
    }

    @Test
    public void test() throws Exception {
        stb = new SafeTeleportBuilder(plugin);
        stb.build();
        SafeSpotTeleport ttt = new SafeSpotTeleport(plugin, player, loc, null, false, 0);
        assertEquals(sst, ttt);
    }

    @Test
    public void testSafeTeleportBuilder() {
        stb = new SafeTeleportBuilder(plugin);
        // Should fail because no data
        assertNull(stb.build());  
    }

    @Test
    public void testEntity() throws Exception {
        // Start builder
        stb = new SafeTeleportBuilder(plugin);
        // Add entity
        stb.entity(player);
        // Test for error
        assertNull(stb.build());
        // Add location
        stb.location(loc);
        // Build - expect success
        SafeSpotTeleport result = stb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testIsland() {
        // Start builder
        SafeTeleportBuilder stb = new SafeTeleportBuilder(plugin);
        // Add entity
        stb.entity(player);
        // Add island
        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(loc);
        stb.island(island);
        // Build - expect success
        SafeSpotTeleport result = stb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testHomeNumber() {
        // Start builder
        SafeTeleportBuilder stb = new SafeTeleportBuilder(plugin);
        // Add entity
        stb.entity(player);
        // Add location
        stb.location(loc);
        // Add home
        stb.homeNumber(10);
        // Build - expect success
        SafeSpotTeleport result = stb.build();
        assertEquals(sst, result);

    }

    @Test
    public void testPortal() {
        // Start builder
        SafeTeleportBuilder stb = new SafeTeleportBuilder(plugin);
        // Add entity
        stb.entity(player);
        // Add location
        stb.location(loc);
        // Portal
        stb.portal();
        // Build - expect success
        SafeSpotTeleport result = stb.build();
        assertEquals(sst, result);
    }

    @Test
    public void testFailureMessage() {
        // Start builder
        SafeTeleportBuilder stb = new SafeTeleportBuilder(plugin);
        // Add entity
        stb.entity(player);
        // Add location
        stb.location(loc);
        // Add failure
        stb.failureMessage("testing 123");
        // Build - expect success
        SafeSpotTeleport result = stb.build();
        assertEquals(sst, result);
    }

}
