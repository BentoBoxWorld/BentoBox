package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class })
public class PortalTeleportationListenerTest {

    private BentoBox plugin;
    private IslandsManager im;
    private PlayersManager pm;
    private IslandWorldManager iwm;
    private World world;
    private World nether;
    private World end;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // island world mgr
        iwm = mock(IslandWorldManager.class);
        world = mock(World.class);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        end = mock(World.class);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        Location endSpawn = mock(Location.class);
        when(endSpawn.getWorld()).thenReturn(end);
        when(end.getSpawnLocation()).thenReturn(endSpawn);
        when(iwm.getEndWorld(Mockito.any())).thenReturn(end);
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(true);
        when(iwm.getIslandWorld(Mockito.any())).thenReturn(world);
        when(iwm.getNetherWorld(Mockito.any())).thenReturn(nether);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getNetherSpawnRadius(Mockito.any())).thenReturn(100);
        when(plugin.getIWM()).thenReturn(iwm);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Set up nether spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(netherSpawn.getWorld()).thenReturn(nether);
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        User user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        UUID notUUID = UUID.randomUUID();
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
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(uuid);
        Optional<Island> optionalIsland = Optional.empty();
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);
        when(plugin.getIslands()).thenReturn(im);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Normally in world
        Util.setPlugin(plugin);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

    }

    private void wrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNotEnd() {
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        // Wrong cause
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.CHORUS_FRUIT);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNoEndWorldGenerated() {
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        // No end world
        when(iwm.isEndGenerate(Mockito.any())).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.END_PORTAL);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalWrongWorld() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location loc = mock(Location.class);

        // Right cause, end exists, wrong world
        when(loc.getWorld()).thenReturn(mock(World.class));
        wrongWorld();
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, null, TeleportCause.END_PORTAL);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNullWorld() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(null);
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, null, TeleportCause.END_PORTAL);
        assertFalse(np.onEndIslandPortal(e));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalHome() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from end
        when(from.getWorld()).thenReturn(end);

        // Player has no island
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        // Right cause, end exists, right world
        PlayerPortalEvent e = new PlayerPortalEvent(player, from, null, null, TeleportCause.END_PORTAL);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        np.onEndIslandPortal(e);
        assertTrue(e.isCancelled());
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNonBentoBoxWorld() {
        when(iwm.inWorld(Mockito.any(World.class))).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(mock(World.class));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onEndIslandPortal(e));
        // Verify
        assertFalse(e.isCancelled());
        Mockito.verify(iwm, Mockito.never()).isEndGenerate(Mockito.any());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortal() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Entity ent = mock(Entity.class);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        // Not in world
        wrongWorld();
        EntityPortalEvent e = new EntityPortalEvent(ent, from, null, null);
        np.onEntityPortal(e);
        assertFalse(e.isCancelled());
        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        e = new EntityPortalEvent(ent, from, null, null);
        np.onEntityPortal(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNotPortal() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        PlayerPortalEvent e = new PlayerPortalEvent(null, null, null, null, TeleportCause.COMMAND);
        assertFalse(np.onNetherPortal(e));
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalWrongWorld() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        wrongWorld();
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIsland() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandWithSpawnDefined() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);

        Island island = mock(Island.class);
        Location spawnLoc = mock(Location.class);
        when(island.getSpawnPoint(Mockito.any())).thenReturn(spawnLoc);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandWithNoSpawnDefined() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);

        Island island = mock(Island.class);
        when(island.getSpawnPoint(Mockito.any())).thenReturn(null);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherStandard() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(false);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     * @throws Exception
     */
    @Test
    public void testOnNetherPortalFromNetherStandard() throws Exception {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(nether);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());

        PlayerPortalEvent e = new PlayerPortalEvent(p, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(false);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);

        // Player should be teleported to their island
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromNetherIsland() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(nether);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        when(iwm.isNetherGenerate(Mockito.any())).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If regular nether, then to = island location
        Mockito.verify(from).toVector();
        Mockito.verify(im, Mockito.never()).getIslandLocation(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherIslandPortalNullLocation() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location loc = null;
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, null, TeleportCause.END_PORTAL);
        assertFalse(np.onNetherPortal(e));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNullWorld() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(null);
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNonBentoBoxWorld() {
        when(iwm.inWorld(Mockito.any(World.class))).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(mock(World.class));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertFalse(e.isCancelled());
        Mockito.verify(iwm, Mockito.never()).isNetherGenerate(Mockito.any());
    }


}
