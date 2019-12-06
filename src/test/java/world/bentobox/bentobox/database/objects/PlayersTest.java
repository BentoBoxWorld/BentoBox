package world.bentobox.bentobox.database.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
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
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class PlayersTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    private Players p;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        when(iwm.getDeathsMax(Mockito.any())).thenReturn(3);
        when(plugin.getIWM()).thenReturn(iwm);

        Server server = mock(Server.class);
        PowerMockito.mockStatic(Bukkit.class);

        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);
        when(Bukkit.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        // world
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Island manager
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        Island island = mock(Island.class);
        UUID uuid = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        ImmutableSet<UUID> set = ImmutableSet.of(uuid, uuid2, uuid3);
        when(island.getMemberSet()).thenReturn(set);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Player manager
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getDeaths(any(), any())).thenReturn(25);
        when(plugin.getPlayers()).thenReturn(pm);

        // Player
        p = new Players(plugin, uuid);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testPlayersBSkyBlockUUID() {
        assertNotNull(new Players(plugin, UUID.randomUUID()));
    }

    @Test
    public void testSetHomeLocationLocation() {
        Location l = mock(Location.class);
        when(l.getWorld()).thenReturn(world);
        p.setHomeLocation(l, 5);
        assertEquals(l, p.getHomeLocation(world, 5));
        assertNotEquals(l, p.getHomeLocation(world, 0));
        p.clearHomeLocations(world);
        assertTrue(p.getHomeLocations(world).isEmpty());
    }

    @Test
    public void testDeaths() {
        assertTrue(p.getDeaths(world) == 0);
        p.addDeath(world);
        assertTrue(p.getDeaths(world) == 1);
        p.addDeath(world);
        assertTrue(p.getDeaths(world) == 2);
        p.addDeath(world);
        assertTrue(p.getDeaths(world) == 3);
        p.addDeath(world);
        assertTrue(p.getDeaths(world) == 3);
        p.addDeath(world);
        assertTrue(p.getDeaths(world) == 3);
        p.setDeaths(world, 10);
        assertTrue(p.getDeaths(world) == 3);
        p.setDeaths(world, 0);
        assertTrue(p.getDeaths(world) == 0);
    }

    /**
     * Test for {@link world.bentobox.bentobox.database.objects.Players#getDeaths(World)}
     */
    @Test
    public void testGetDeaths() {
        p.addDeath(world);
        p.addDeath(world);
        assertEquals(2, p.getDeaths(world));
    }

}
