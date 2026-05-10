package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class IslandRespawnListenerTest extends CommonTestSetup {

    @Mock
    private Location safeLocation;

    @Mock
    private Location islandCenter;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // World
        
        when(world.getUID()).thenReturn(uuid);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        mockedBukkit.when(() -> Bukkit.getWorld(uuid)).thenReturn(world);
        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockPlayer.getLocation()).thenReturn(mock(Location.class));
        when(mockPlayer.getServer()).thenReturn(server);
        when(mockPlayer.getName()).thenReturn("tasty");

        // Island World Manager
        // All locations are in world by default
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma);
        when(iwm.getAddon(any())).thenReturn(opGma);
        safeLocation = mock(Location.class);
        when(safeLocation.getWorld()).thenReturn(world);
        when(safeLocation.clone()).thenReturn(safeLocation);
        
        // Island Manager
        when(im.getHomeLocation(eq(world), any(UUID.class))).thenReturn(safeLocation);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.isSafeLocation(safeLocation)).thenReturn(true);

        User.setPlugin(plugin);
        User.getInstance(mockPlayer);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeathNotIslandWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeathNoFlag() {
        Flags.ISLAND_RESPAWN.setSetting(world, false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeathNotOwnerNotTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world, never()).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeathNotOwnerInTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeathOwnerNoTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnPlayerDeath() {
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        new IslandRespawnListener().onPlayerDeath(e);
        verify(world).getUID();
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    void testOnPlayerRespawn() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        assertEquals(safeLocation, ev.getRespawnLocation());
        // Verify commands
        mockedUtil.verify(() -> Util.runCommands(any(User.class), anyString(), eq(Collections.emptyList()), eq("respawn")));
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    void testOnPlayerRespawnWithoutDeath() {
        IslandRespawnListener l = new IslandRespawnListener();
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        assertSame(location, ev.getRespawnLocation());
    }

    /**
     * Test method for {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    void testOnPlayerRespawnWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        assertSame(location, ev.getRespawnLocation());
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     * When the home location is not safe but one block above is safe, the player
     * should respawn one block above the home.
     */
    @Test
    void testOnPlayerRespawnUnsafeHomeOneAboveSafe() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Make clone().add() return the same mock (simulating one block above)
        when(safeLocation.add(any(Vector.class))).thenReturn(safeLocation);
        // Home is unsafe on first check, but safe on subsequent checks (one block above)
        when(im.isSafeLocation(safeLocation)).thenReturn(false, true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        // Player should respawn at lPlusOne (which is same mock object as safeLocation after clone/add)
        assertEquals(safeLocation, ev.getRespawnLocation());
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     * When the home location and one block above are both unsafe, the player should
     * respawn at a safe location found on the island center.
     */
    @Test
    void testOnPlayerRespawnUnsafeHomeFallsBackToIslandCenter() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Home and one-above are both unsafe
        when(im.isSafeLocation(safeLocation)).thenReturn(false);
        // Island center is available and safe
        when(islandCenter.getWorld()).thenReturn(world);
        when(islandCenter.clone()).thenReturn(islandCenter);
        when(islandCenter.add(any(Vector.class))).thenReturn(islandCenter);
        when(im.getIslandLocation(eq(world), any(UUID.class))).thenReturn(islandCenter);
        when(im.isSafeLocation(islandCenter)).thenReturn(true);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        // Player should respawn at island center (or an offset from it)
        assertEquals(islandCenter, ev.getRespawnLocation());
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     * When the home location is unsafe and no safe location can be found on the island,
     * and the player has no island, the respawn location should remain unchanged.
     */
    @Test
    void testOnPlayerRespawnUnsafeHomeNoSafeIslandLocation() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Home is unsafe, no island location, and getIsland returns null (no island)
        when(im.isSafeLocation(safeLocation)).thenReturn(false);
        when(im.getIslandLocation(eq(world), any(UUID.class))).thenReturn(null);
        when(im.getIsland(any(World.class), any(User.class))).thenReturn(null);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        // No island: location unchanged and SafeSpotTeleport should NOT be scheduled
        assertSame(location, ev.getRespawnLocation());
        verify(sch, never()).runTask(any(), any(Runnable.class));
    }

    /**
     * Test method for
     * {@link IslandRespawnListener#onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     * When all quick sync checks fail but the player has an island, the respawn
     * location should be set to the island protection center and SafeSpotTeleport
     * should be scheduled to find a truly safe spot after respawn.
     */
    @Test
    void testOnPlayerRespawnSafeSpotTeleportFallback() {
        // Die
        List<ItemStack> drops = new ArrayList<>();
        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, drops, 0, 0, 0, 0, "");
        IslandRespawnListener l = new IslandRespawnListener();
        l.onPlayerDeath(e);
        // Has island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Home, one-above, and center offsets are all unsafe
        when(im.isSafeLocation(safeLocation)).thenReturn(false);
        when(safeLocation.add(any(Vector.class))).thenReturn(safeLocation);
        when(islandCenter.clone()).thenReturn(islandCenter);
        when(islandCenter.add(any(Vector.class))).thenReturn(islandCenter);
        when(im.getIslandLocation(eq(world), any(UUID.class))).thenReturn(islandCenter);
        when(im.isSafeLocation(islandCenter)).thenReturn(false);
        // Player has an island for SafeSpotTeleport fallback
        when(im.getIsland(any(World.class), any(User.class))).thenReturn(island);
        Location protectionCenter = mock(Location.class);
        when(protectionCenter.getWorld()).thenReturn(world);
        when(protectionCenter.clone()).thenReturn(protectionCenter);
        when(island.getProtectionCenter()).thenReturn(protectionCenter);
        // Respawn
        PlayerRespawnEvent ev = new PlayerRespawnEvent(mockPlayer, location, false, false, false, RespawnReason.DEATH);
        l.onPlayerRespawn(ev);
        // Respawn location should be set to island protection center as best-effort
        assertEquals(protectionCenter, ev.getRespawnLocation());
        // SafeSpotTeleport should be scheduled to run on next tick
        verify(sch).runTask(eq(plugin), any(Runnable.class));
    }
}

