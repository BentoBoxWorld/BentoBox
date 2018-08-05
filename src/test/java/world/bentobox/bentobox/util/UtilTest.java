/**
 *
 */
package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class })
public class UtilTest {

    private BentoBox plugin;
    private World world;
    private IslandWorldManager iwm;
    private Location location;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Util.setPlugin(plugin);
        // World
        world = mock(World.class);
        when(world.getName()).thenReturn("world_name");
        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(500D);
        when(location.getY()).thenReturn(600D);
        when(location.getZ()).thenReturn(700D);
        when(location.getBlockX()).thenReturn(500);
        when(location.getBlockY()).thenReturn(600);
        when(location.getBlockZ()).thenReturn(700);
        when(location.getYaw()).thenReturn(10F);
        when(location.getPitch()).thenReturn(20F);

        PowerMockito.mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(server.getWorld(Mockito.anyString())).thenReturn(world);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getServerVersion()}.
     */
    @Test
    public void testGetServerVersion() {
        assertEquals("bukkit",Util.getServerVersion());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getClosestIsland(org.bukkit.Location)}.
     */
    @Test
    public void testGetClosestIsland() throws Exception {
        Util.setPlugin(plugin);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(world)).thenReturn(100);
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        when(iwm.getIslandHeight(world)).thenReturn(120);
        when(location.getBlockX()).thenReturn(456);
        when(location.getBlockZ()).thenReturn(456);
        Location l = Util.getClosestIsland(location);
        assertEquals(500, l.getBlockX());
        assertEquals(500, l.getBlockZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getLocationString(java.lang.String)}.
     */
    @Test
    public void testGetLocationString() {
        assertNull(Util.getLocationString(null));
        assertNull(Util.getLocationString(""));
        assertNull(Util.getLocationString("     "));
        Location result = Util.getLocationString("world_name:500:600:700.0:1092616192:1101004800");
        assertEquals(world, result.getWorld());
        assertTrue(result.getX() == 500.5D);
        assertTrue(result.getY() == 600D);
        assertTrue(result.getZ() == 700.5D);
        assertTrue(result.getYaw() == 10F);
        assertTrue(result.getPitch() == 20F);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getStringLocation(org.bukkit.Location)}.
     */
    @Test
    public void testGetStringLocation() {
        assertEquals("", Util.getStringLocation(null));
        when(location.getWorld()).thenReturn(null);
        assertEquals("", Util.getStringLocation(location));
        when(location.getWorld()).thenReturn(world);
        assertEquals("world_name:500:600:700:1092616192:1101004800", Util.getStringLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#prettifyText(java.lang.String)}.
     */
    @Test
    public void testPrettifyText() {
        assertEquals("Hello There This Is A Test", Util.prettifyText("HELLO_THERE_THIS_IS_A_TEST"));
        assertEquals("All caps test", Util.prettifyText("ALL CAPS TEST"));
        assertEquals("First capital letter", Util.prettifyText("first capital letter"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getOnlinePlayerList(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetOnlinePlayerList() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#tabLimit(java.util.List, java.lang.String)}.
     */
    @Test
    public void testTabLimit() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getPermValue(org.bukkit.entity.Player, java.lang.String, int)}.
     */
    @Test
    public void testGetPermValue() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#xyz(org.bukkit.util.Vector)}.
     */
    @Test
    public void testXyz() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#sameWorld(org.bukkit.World, org.bukkit.World)}.
     */
    @Test
    public void testSameWorld() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetWorld() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#blockFaceToFloat(org.bukkit.block.BlockFace)}.
     */
    @Test
    public void testBlockFaceToFloat() {
        //fail("Not yet implemented"); // TODO
    }

}
