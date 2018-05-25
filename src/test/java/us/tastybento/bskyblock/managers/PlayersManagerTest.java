/**
 * 
 */
package us.tastybento.bskyblock.managers;

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
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Names;
import us.tastybento.bskyblock.database.objects.Players;
import us.tastybento.bskyblock.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class, Util.class, PlayersManager.class })
public class PlayersManagerTest {

    private BSkyBlock plugin;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private UUID notUUID;
    private BukkitScheduler sch;
    private IslandWorldManager iwm;
    private World world;
    private World nether;
    private World end;
    private BSBDatabase<?> db;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
        // island world mgr
        iwm = mock(IslandWorldManager.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("world");
        nether = mock(World.class);
        when(nether.getName()).thenReturn("world_nether");
        end = mock(World.class);
        when(end.getName()).thenReturn("world_the_end");
        when(iwm.getEndWorld()).thenReturn(end);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getNetherWorld()).thenReturn(nether);
        when(iwm.inWorld(any())).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);
        
        // Settings
        s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);
        
        // Set up spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);
        when(s.getNetherSpawnRadius()).thenReturn(100);

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

        // Player has island to begin with 
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);


        // Server & Scheduler
        sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        
        // Normally in world
        Util.setPlugin(plugin);
              
        // Mock database
        db = mock(BSBDatabase.class);
        PowerMockito.whenNew(BSBDatabase.class).withAnyArguments().thenReturn(db);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#PlayersManager(us.tastybento.bskyblock.BSkyBlock)}.
     */
    @Test
    public void testPlayersManager() {
        PlayersManager pm = new PlayersManager(plugin);
        assertNotNull(pm);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#load()}.
     */
    @Test
    public void testLoad() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.load();
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#save(boolean)}.
     */
    @Test
    public void testSaveBoolean() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.save(false);
        pm.save(true);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        PlayersManager pm = new PlayersManager(plugin);
        pm.shutdown();
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#getPlayer(java.util.UUID)}.
     */
    @Test
    public void testGetPlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);
        Players player = pm.getPlayer(uuid);
        assertEquals(uuid.toString(), player.getPlayerName());
        assertEquals(uuid.toString(), player.getUniqueId());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#addPlayer(java.util.UUID)}.
     */
    @Test
    public void testAddPlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        pm.addPlayer(null);
        // Add twice
        assertFalse(pm.isKnown(uuid));
        pm.addPlayer(uuid);
        assertTrue(pm.isKnown(uuid));
        pm.addPlayer(uuid);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#removeOnlinePlayer(java.util.UUID)}.
     */
    @Test
    public void testRemoveOnlinePlayer() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        assertFalse(pm.isKnown(uuid));
        pm.removeOnlinePlayer(uuid);
        pm.addPlayer(uuid);
        pm.removeOnlinePlayer(uuid);
        assertFalse(pm.isKnown(uuid));
        pm.removeOnlinePlayer(uuid);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#removeAllPlayers()}.
     */
    @Test
    public void testRemoveAllPlayers() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        pm.addPlayer(uuid);
        pm.addPlayer(notUUID);
        
        pm.removeAllPlayers();
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#isKnown(java.util.UUID)}.
     */
    @Test
    public void testIsKnown() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        pm.addPlayer(uuid);
        pm.addPlayer(notUUID);

        assertFalse(pm.isKnown(null));
        assertTrue(pm.isKnown(uuid));
        assertTrue(pm.isKnown(notUUID));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#setHomeLocation(User, org.bukkit.Location, int)}.
     */
    @Test
    public void testSetAndGetHomeLocationUserLocationInt() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

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
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#clearHomeLocations(java.util.UUID)}.
     */
    @Test
    public void testClearHomeLocations() {
        PlayersManager pm = new PlayersManager(plugin);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getName()).thenReturn("tasty");
        when(server.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

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
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#getUUID(java.lang.String)}.
     */
    @Test
    public void testGetUUID() {
        PlayersManager pm = new PlayersManager(plugin);
        assertEquals(uuid,pm.getUUID(uuid.toString()));
        
        OfflinePlayer olp = mock(OfflinePlayer.class);
        when(olp.getUniqueId()).thenReturn(uuid);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOfflinePlayer(Mockito.any(UUID.class))).thenReturn(olp);

        Names name = mock(Names.class);
        when(name.getUuid()).thenReturn(uuid);

        // Database
        when(db.objectExists(Mockito.anyString())).thenReturn(true);
        when(db.loadObject(Mockito.anyString())).thenAnswer(new Answer<Names>() {

            @Override
            public Names answer(InvocationOnMock invocation) throws Throwable {
                
                return name;
            }
            
        });
        assertEquals(uuid, pm.getUUID("tastybento"));
        
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#setPlayerName(us.tastybento.bskyblock.api.user.User)}.
     */
    @Test
    public void testSetandGetPlayerName() {
        PlayersManager pm = new PlayersManager(plugin);
        // Add a player
        pm.addPlayer(uuid);
        assertEquals(uuid.toString(), pm.getName(user.getUniqueId()));
        pm.setPlayerName(user);
        assertEquals(user.getName(), pm.getName(user.getUniqueId()));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#getResetsLeft(java.util.UUID)}.
     */
    @Test
    public void testGetSetResetsLeft() {
        PlayersManager pm = new PlayersManager(plugin);
        // Add a player
        pm.addPlayer(uuid);
        assertEquals(0, pm.getResetsLeft(uuid));
        pm.setResetsLeft(uuid, 20);
        assertEquals(20, pm.getResetsLeft(uuid));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.PlayersManager#save(java.util.UUID)}.
     */
    @Test
    public void testSaveUUID() {
        PlayersManager pm = new PlayersManager(plugin);
        // Add a player
        pm.addPlayer(uuid);
        pm.save(uuid);
        Mockito.verify(db).saveObject(Mockito.any());
    }

}
