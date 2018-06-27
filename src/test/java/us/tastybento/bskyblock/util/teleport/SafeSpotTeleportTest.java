/**
 * 
 */
package us.tastybento.bskyblock.util.teleport;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { BSkyBlock.class })
public class SafeSpotTeleportTest {

    @Mock
    static BSkyBlock plugin;
    @Mock
    private static World world;
    @Mock
    private static BukkitScheduler sch;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        world = mock(World.class);
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");
        
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
        plugin = mock(BSkyBlock.class);
        // Users
        User.setPlugin(plugin);
        // Locales - final        
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");
        
        // Island Manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        
        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(mock(Location.class));
        
        // Default is that there is no island around here
        Optional<Island> oi = Optional.empty();
        when(im.getIslandAt(Mockito.any())).thenReturn(oi);
        
        // Settings
        Settings settings = mock(Settings.class);
        when(settings.getIslandProtectionRange()).thenReturn(1);
        when(plugin.getSettings()).thenReturn(settings);
        
        // Island world manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getIslandProtectionRange(Mockito.any())).thenReturn(1);
        when(plugin.getIWM()).thenReturn(iwm);
        
        // Server & Scheduler
        sch = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(sch);
        
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.util.teleport.SafeSpotTeleport#SafeSpotTeleport(us.tastybento.bskyblock.BSkyBlock, org.bukkit.entity.Entity, org.bukkit.Location, java.lang.String, boolean, int)}.
     */
    @Test
    public void testSafeSpotTeleport() throws Exception {
        
        Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(0);
        when(loc.getBlockY()).thenReturn(120);
        when(loc.getBlockZ()).thenReturn(0);
        Block block = mock(Block.class);
        when(loc.getBlock()).thenReturn(block);
        boolean portal = false;
        int homeNumber = 1;
        new SafeSpotTeleport(plugin, player, loc, "failure message", portal, homeNumber);
        
    }

}
