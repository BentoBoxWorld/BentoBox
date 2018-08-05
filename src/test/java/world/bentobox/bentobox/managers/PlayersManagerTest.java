/**
 *
 */
package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.BSBDatabase;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class})
public class PlayersManagerTest {

    private BentoBox plugin;
    private UUID uuid;
    private User user;
    private UUID notUUID;
    private World world;
    private World nether;
    private World end;
    @Mock
    private BSBDatabase<Players> db;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // island world mgr
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("world");
        nether = mock(World.class);
        when(nether.getName()).thenReturn("world_nether");
        end = mock(World.class);
        when(end.getName()).thenReturn("world_the_end");
        when(iwm.inWorld(any())).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Set up spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);
        when(iwm.getNetherSpawnRadius(Mockito.any())).thenReturn(100);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);


        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getUniqueId()).thenReturn(uuid);
        when(olp.getName()).thenReturn("tastybento");
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);


        // Player has island to begin with
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);


        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Normally in world
        Util.setPlugin(plugin);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#PlayersManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testPlayersManager() {
        PlayersManager pm = new PlayersManager(plugin);
        assertNotNull(pm);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#load()}.
     */
    @Test
    public void testLoad() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.setHandler(db);
        pm.load();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#save(boolean)}.
     */
    @Test
    public void testSaveBoolean() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.save(false);
        pm.save(true);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.shutdown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getPlayer(java.util.UUID)}.
     */
    @Test
    public void testGetPlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        Players player = pm.getPlayer(uuid);
        assertEquals("tastybento", player.getPlayerName());
        assertEquals(uuid.toString(), player.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#addPlayer(java.util.UUID)}.
     */
    @Test
    public void testAddPlayer() {
        PlayersManager pm = new PlayersManager(plugin);

        pm.addPlayer(null);
        // Add twice
        assertFalse(pm.isKnown(uuid));
        pm.addPlayer(uuid);
        assertTrue(pm.isKnown(uuid));
        pm.addPlayer(uuid);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#removeOnlinePlayer(java.util.UUID)}.
     */
    @Test
    public void testRemoveOnlinePlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.setHandler(db);
        // Unknown UUID
        assertFalse(pm.isKnown(uuid));
        // Remove it just to check this does not fail
        pm.removeOnlinePlayer(uuid);
        // Should still be unknown
        assertFalse(pm.isKnown(uuid));
        // Add the player
        pm.addPlayer(uuid);
        // Now should be known
        assertTrue(pm.isKnown(uuid));
        // Remove the player
        pm.removeOnlinePlayer(uuid);
        // There's no check possible to confirm right now.
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#removeAllPlayers()}.
     */
    @Test
    public void testRemoveAllPlayers() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.setHandler(db);

        pm.addPlayer(uuid);
        pm.addPlayer(notUUID);

        pm.removeAllPlayers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#isKnown(java.util.UUID)}.
     */
    @Test
    public void testIsKnown() {
        PlayersManager pm = new PlayersManager(plugin);

        pm.addPlayer(uuid);
        pm.addPlayer(notUUID);

        assertFalse(pm.isKnown(null));
        assertTrue(pm.isKnown(uuid));
        assertTrue(pm.isKnown(notUUID));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#setHomeLocation(User, org.bukkit.Location, int)}.
     */
    @Test
    public void testSetAndGetHomeLocationUserLocationInt() {
        PlayersManager pm = new PlayersManager(plugin);

        Location l = mock(Location.class);
        when(l.getWorld()).thenReturn(world);
        Location l2 = mock(Location.class);
        when(l2.getWorld()).thenReturn(nether);
        Location l3 = mock(Location.class);
        when(l3.getWorld()).thenReturn(end);

        pm.setHomeLocation(uuid, l, 1);
        pm.setHomeLocation(uuid, l2, 0);
        pm.setHomeLocation(uuid, l3, 10);
        assertEquals(l, pm.getHomeLocation(world, uuid));
        assertEquals(l2, pm.getHomeLocation(world, uuid, 0));
        assertEquals(l3, pm.getHomeLocation(world, uuid, 10));
        assertNotEquals(l, pm.getHomeLocation(world, uuid, 20));
    }

    /**
     * Test method for .
     */
    @Test
    public void testClearHomeLocations() {
        PlayersManager pm = new PlayersManager(plugin);

        Location l = mock(Location.class);
        when(l.getWorld()).thenReturn(world);
        Location l2 = mock(Location.class);
        when(l2.getWorld()).thenReturn(nether);
        Location l3 = mock(Location.class);
        when(l3.getWorld()).thenReturn(end);
        pm.setHomeLocation(uuid, l, 1);
        pm.setHomeLocation(uuid, l2, 0);
        pm.setHomeLocation(uuid, l3, 10);
        assertFalse(pm.getHomeLocations(world, uuid).isEmpty());
        pm.clearHomeLocations(world, uuid);
        assertTrue(pm.getHomeLocations(world, uuid).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDwithUUID() {
        PlayersManager pm = new PlayersManager(plugin);
        assertEquals(uuid,pm.getUUID(uuid.toString()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUIDOfflinePlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.setHandler(db);
        // Add a player to the cache
        pm.addPlayer(uuid);
        //assertEquals(uuid, pm.getUUID("tastybento")); TODO Fix NPE
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#setPlayerName(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testSetandGetPlayerName() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.setHandler(db);
        // Add a player
        pm.addPlayer(uuid);
        assertEquals("tastybento", pm.getName(user.getUniqueId()));
        //pm.setPlayerName(user); TODO: fine NPE
        assertEquals(user.getName(), pm.getName(user.getUniqueId()));
    }

    /**
     * Test method for .
     */
    @Test
    public void testGetSetResetsLeft() {
        PlayersManager pm = new PlayersManager(plugin);

        // Add a player
        pm.addPlayer(uuid);
        assertEquals(0, pm.getResets(world, uuid));
        pm.setResets(world, uuid, 20);
        assertEquals(20, pm.getResets(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.PlayersManager#save(java.util.UUID)}.
     */
    @Test
    public void testSaveUUID() {
        PlayersManager pm = new PlayersManager(plugin);
        // Add a player
        pm.addPlayer(uuid);
        //pm.save(uuid);
    }

}
