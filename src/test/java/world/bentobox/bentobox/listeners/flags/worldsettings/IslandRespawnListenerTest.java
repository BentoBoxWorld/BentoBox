package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
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
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class })
public class IslandRespawnListenerTest {

    @Mock
    private World world;
    @Mock
    private Player player;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Location safeLocation;
    @Mock
    private Server server;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(server.getWorld(any(UUID.class))).thenReturn(world);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(mock(Location.class));
        when(player.getServer()).thenReturn(server);

        // Island World Manager
        // All locations are in world by default
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);

        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);
        safeLocation = mock(Location.class);
        when(safeLocation.getWorld()).thenReturn(world);
        when(im.getSafeHomeLocation(any(), any(), Mockito.anyInt())).thenReturn(safeLocation);

        // Sometimes use Mockito.withSettings().verboseLogging()
        User.setPlugin(plugin);
        User.getInstance(player);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeathNotIslandWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeathNoFlag() {
        Flags.ISLAND_RESPAWN.setSetting(world, false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeathNotOwnerNotTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeathNotOwnerInTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeathOwnerNoTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    public void testOnPlayerDeath() {
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnPlayerRespawn() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(player, location, false);
        l.onPlayerRespawn(ev);
        assertEquals(safeLocation, ev.getRespawnLocation());
        // Verify commands
        PowerMockito.verifyStatic(Util.class);
        Util.runCommands(any(User.class), eq(Collections.emptyList()), eq("respawn"));
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnPlayerRespawnWithoutDeath() {
        IslandRespawnListener l = new IslandRespawnListener();
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(player, location, false);
        l.onPlayerRespawn(ev);
        assertEquals(location, ev.getRespawnLocation());
    }


    /**
     * Test method for {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnPlayerRespawnWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(player, location, false);
        l.onPlayerRespawn(ev);
        assertEquals(location, ev.getRespawnLocation());
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnPlayerRespawnFlagNotSet() {
        Flags.ISLAND_RESPAWN.setSetting(world, false);
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = new PlayerDeathEvent(player, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(player, location, false);
        l.onPlayerRespawn(ev);
        assertEquals(location, ev.getRespawnLocation());
    }
}
