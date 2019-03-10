/**
 *
 */
package world.bentobox.bentobox.util.teleport;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
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
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { BentoBox.class, Bukkit.class })
public class SafeSpotTeleportTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private BukkitScheduler sch;
    @Mock
    private IslandsManager im;
    @Mock
    private Player player;
    @Mock
    private Location loc;


    @Before
    public void setUp() throws Exception {
        // Bukkit and scheduler
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.any(Long.class),Mockito.any(Long.class))).thenReturn(task);

        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Users
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        // Safe location
        when(im.isSafeLocation(Mockito.any())).thenReturn(true);

        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(mock(Location.class));

        // Default is that there is no island around here
        Optional<Island> oi = Optional.empty();
        when(im.getIslandAt(Mockito.any())).thenReturn(oi);

        // Island world manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getIslandProtectionRange(Mockito.any())).thenReturn(1);
        when(iwm.getDefaultGameMode(Mockito.any())).thenReturn(GameMode.SURVIVAL);
        when(plugin.getIWM()).thenReturn(iwm);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        // Player
        // Return first survival and then spectator
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL, GameMode.SPECTATOR);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(0);
        when(loc.getBlockY()).thenReturn(120);
        when(loc.getBlockZ()).thenReturn(0);
        Block block = mock(Block.class);
        when(loc.getBlock()).thenReturn(block);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#SafeSpotTeleport(world.bentobox.bentobox.BentoBox, org.bukkit.entity.Entity, org.bukkit.Location, java.lang.String, boolean, int)}.
     */
    @Test
    public void testSafeSpotTeleportImmediateSafe() throws Exception {
        boolean portal = false;
        int homeNumber = 1;
        new SafeSpotTeleport(plugin, player, loc, "failure message", portal, homeNumber, true);
        Mockito.verify(player).setGameMode(GameMode.SPECTATOR);
        Mockito.verify(player).teleport(loc);
        Mockito.verify(player).setGameMode(GameMode.SURVIVAL);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#SafeSpotTeleport(world.bentobox.bentobox.BentoBox, org.bukkit.entity.Entity, org.bukkit.Location, java.lang.String, boolean, int)}.
     */
    @Test
    public void testSafeSpotTeleportImmediateSafeNoOverride() throws Exception {
        boolean portal = false;
        int homeNumber = 1;
        new SafeSpotTeleport(plugin, player, loc, "failure message", portal, homeNumber, false);
        Mockito.verify(player, Mockito.never()).setGameMode(GameMode.SPECTATOR);
        Mockito.verify(player).teleport(loc);
        Mockito.verify(player, Mockito.never()).setGameMode(GameMode.SURVIVAL);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.teleport.SafeSpotTeleport#SafeSpotTeleport(world.bentobox.bentobox.BentoBox, org.bukkit.entity.Entity, org.bukkit.Location, java.lang.String, boolean, int)}.
     */
    @Test
    public void testSafeSpotTeleportNotImmediatelySafe() throws Exception {
        when(im.isSafeLocation(Mockito.any())).thenReturn(false);
        boolean portal = false;
        int homeNumber = 1;
        new SafeSpotTeleport(plugin, player, loc, "failure message", portal, homeNumber, true);
        Mockito.verify(player).setGameMode(GameMode.SPECTATOR);
        Mockito.verify(player, Mockito.never()).teleport(loc);
        Mockito.verify(sch).runTaskTimer(Mockito.any(), Mockito.any(Runnable.class), Mockito.eq(0L), Mockito.eq(1L));

    }

}
