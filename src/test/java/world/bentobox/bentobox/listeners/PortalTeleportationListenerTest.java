package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import org.eclipse.jdt.annotation.Nullable;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
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
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class, BlueprintPaster.class })
public class PortalTeleportationListenerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private IslandsManager im;
    private PlayersManager pm;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World world;
    @Mock
    private World nether;
    @Mock
    private World end;
    @Mock
    private Player p;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private GameModeAddon gameModeAddon;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // island world mgr
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        Location endSpawn = mock(Location.class);
        when(endSpawn.getWorld()).thenReturn(end);
        when(end.getSpawnLocation()).thenReturn(endSpawn);
        when(iwm.getEndWorld(any())).thenReturn(end);
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.getIslandWorld(any())).thenReturn(world);
        when(iwm.getNetherWorld(any())).thenReturn(nether);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getNetherSpawnRadius(any())).thenReturn(100);
        when(plugin.getIWM()).thenReturn(iwm);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Set up nether spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(netherSpawn.getWorld()).thenReturn(nether);
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);

        // Player
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
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.isOwner(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        Optional<Island> optionalIsland = Optional.empty();
        when(im.getIslandAt(any())).thenReturn(optionalIsland);
        when(plugin.getIslands()).thenReturn(im);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Normally in world
        Util.setPlugin(plugin);

        // Addon
        Optional<GameModeAddon> opAddon = Optional.of(gameModeAddon);
        when(iwm.getAddon(any())).thenReturn(opAddon);

        // Blueprints
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        @Nullable
        BlueprintBundle defaultBB = new BlueprintBundle();
        Blueprint bp = new Blueprint();
        bp.setName("blueprintname");
        defaultBB.setBlueprint(World.Environment.NETHER, bp);
        defaultBB.setBlueprint(World.Environment.THE_END, bp);
        when(bpm.getDefaultBlueprintBundle(any())).thenReturn(defaultBB);
        when(bpm.getBlueprints(any())).thenReturn(Collections.singletonMap("blueprintname", bp));
        // Paster

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.CHORUS_FRUIT);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNoEndWorldGenerated() {
        Location from = mock(Location.class);
        // Teleport from world to end
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        // No end world
        when(iwm.isEndGenerate(any())).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.END_PORTAL);
        np.onEndIslandPortal(e);
        assertTrue(e.isCancelled());
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, TeleportCause.END_PORTAL);
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, TeleportCause.END_PORTAL);
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
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        // Right cause, end exists, right world
        PlayerPortalEvent e = new PlayerPortalEvent(player, from, null, TeleportCause.END_PORTAL);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
        // Give player an island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        np.onEndIslandPortal(e);
        assertTrue(e.isCancelled());
        verify(im).homeTeleportAsync(any(), eq(player));
    }

    /**
     * Test method for {@link PortalTeleportationListener#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNonBentoBoxWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(mock(World.class));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onEndIslandPortal(e));
        // Verify
        assertFalse(e.isCancelled());
        verify(iwm, never()).isEndGenerate(any());
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
        EntityPortalEvent e = new EntityPortalEvent(ent, from, null);
        np.onEntityPortal(e);
        assertFalse(e.isCancelled());
        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        e = new EntityPortalEvent(ent, from, null);
        np.onEntityPortal(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNotPortal() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        PlayerPortalEvent e = new PlayerPortalEvent(null, null, null, TeleportCause.COMMAND);
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        verify(from).toVector();
        // Do not go to spawn
        verify(nether, never()).getSpawnLocation();
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandPasteBlueprintError() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        // Paste
        when(iwm.isPasteMissingIslands(any())).thenReturn(true);
        Island isle = mock(Island.class);
        when(isle.getWorld()).thenReturn(world);
        when(isle.hasEndIsland()).thenReturn(false);
        Optional<Island> island = Optional.of(isle );
        when(im.getIslandAt(any())).thenReturn(island);
        // No bp
        when(bpm.getBlueprints(any())).thenReturn(Collections.emptyMap());
        // Test
        assertTrue(np.onNetherPortal(e));
        // Error
        verify(plugin).logError(eq("Could not paste default island in nether or end. Is there a nether-island or end-island blueprint?"));
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandPasteBlueprint() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        // Paste
        when(iwm.isPasteMissingIslands(any())).thenReturn(true);
        Island isle = mock(Island.class);
        when(isle.getWorld()).thenReturn(world);
        when(isle.getCenter()).thenReturn(from);
        when(isle.hasEndIsland()).thenReturn(false);
        Optional<Island> island = Optional.of(isle );
        when(im.getIslandAt(any())).thenReturn(island);
        // Test
        assertTrue(np.onNetherPortal(e));
        // Error
        verify(plugin, never()).logError(eq("Could not paste default island in nether or end. Is there a nether-island or end-island blueprint?"));
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);

        Island island = mock(Island.class);
        Location spawnLoc = mock(Location.class);
        when(island.getSpawnPoint(any())).thenReturn(spawnLoc);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        verify(from).toVector();
        // Do not go to spawn
        verify(nether, never()).getSpawnLocation();
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);

        Island island = mock(Island.class);
        when(island.getSpawnPoint(any())).thenReturn(null);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        verify(from).toVector();
        // Do not go to spawn
        verify(nether, never()).getSpawnLocation();
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(any())).thenReturn(false);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
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

        PlayerPortalEvent e = new PlayerPortalEvent(p, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(any())).thenReturn(false);
        when(iwm.isNetherGenerate(any())).thenReturn(true);

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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If regular nether, then to = island location
        verify(from).toVector();
        verify(im, never()).getIslandLocation(any(), any());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherIslandPortalNullLocation() {
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location loc = null;
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, TeleportCause.END_PORTAL);
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
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PortalTeleportationListener#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNonBentoBoxWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        PortalTeleportationListener np = new PortalTeleportationListener(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(mock(World.class));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
        // Verify
        assertFalse(e.isCancelled());
        verify(iwm, never()).isNetherGenerate(any());
    }


}
