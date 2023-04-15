package world.bentobox.bentobox.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import net.md_5.bungee.api.ChatColor;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class })
public class UtilTest {

    private static final String[] NAMES = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Location location;
    @Mock
    private User user;
    @Mock
    private ConsoleCommandSender sender;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Util.setPlugin(plugin);
        // World
        when(world.getName()).thenReturn("world_name");
        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(500D);
        when(location.getY()).thenReturn(600D);
        when(location.getZ()).thenReturn(700D);
        when(location.getBlockX()).thenReturn(500);
        when(location.getBlockY()).thenReturn(600);
        when(location.getBlockZ()).thenReturn(700);
        when(location.getYaw()).thenReturn(10F);
        when(location.getPitch()).thenReturn(20F);

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getWorld(anyString())).thenReturn(world);
        when(Bukkit.getConsoleSender()).thenReturn(sender);

        // Bukkit - online players
        User.setPlugin(plugin);
        Map<UUID, String> online = new HashMap<>();

        Set<Player> onlinePlayers = new HashSet<>();
        for (String name : NAMES) {
            Player p1 = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p1.getUniqueId()).thenReturn(uuid);
            when(p1.getName()).thenReturn(name);
            when(p1.hasPermission(anyString())).thenReturn(true);
            online.put(uuid, name);
            onlinePlayers.add(p1);
            // Add to User cache
            User.getInstance(p1);
        }
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

        when(user.isPlayer()).thenReturn(true);
        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getServerVersion()}.
     */
    @Test
    public void testGetServerVersion() {
        assertEquals("bukkit", Util.getServerVersion());
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
        assertEquals(400, l.getBlockX());
        assertEquals(400, l.getBlockZ());
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
        assertEquals(500.5D, result.getX(), 0.0);
        assertEquals(600D, result.getY(), 0.0);
        assertEquals(700.5D, result.getZ(), 0.0);
        assertEquals(10F, result.getYaw(), 0.0);
        assertEquals(20F, result.getPitch(), 0.0);
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
        assertEquals("Online players, null", 11, Util.getOnlinePlayerList(null).size());
        assertEquals("Online players, not user", 11, Util.getOnlinePlayerList(mock(User.class)).size());
        Player p = mock(Player.class);
        // Can't see (default)
        when(p.canSee(any(Player.class))).thenReturn(false);
        when(user.getPlayer()).thenReturn(p);
        assertEquals("Online players, cannot see", 0, Util.getOnlinePlayerList(user).size());
        // Can see
        when(p.canSee(any(Player.class))).thenReturn(true);
        assertEquals("Online players, cannot see", 11, Util.getOnlinePlayerList(user).size());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#tabLimit(java.util.List, java.lang.String)}.
     */
    @Test
    public void testTabLimit() {
        List<String> list = new ArrayList<>();
        assertTrue(Util.tabLimit(list, "").isEmpty());
        list.add("alpha");
        list.add("bravo");
        list.add("charlie");
        list.add("delta");
        list.add("epsilon");
        assertEquals(5, Util.tabLimit(list, "").size());
        assertEquals(1, Util.tabLimit(list, "a").size());
        assertEquals(1, Util.tabLimit(list, "b").size());
        assertEquals(1, Util.tabLimit(list, "c").size());
        assertEquals(1, Util.tabLimit(list, "d").size());
        assertEquals(1, Util.tabLimit(list, "e").size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#xyz(org.bukkit.util.Vector)}.
     */
    @Test
    public void testXyz() {
        assertEquals("34,67,54", Util.xyz(new Vector(34, 67, 54)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#sameWorld(org.bukkit.World, org.bukkit.World)}.
     */
    @Test
    public void testSameWorld() {
        World world = mock(World.class);
        World world2 = mock(World.class);
        World world3 = mock(World.class);
        World world4 = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world2.getName()).thenReturn("world_nether");
        when(world2.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(world3.getName()).thenReturn("world_the_end");
        when(world3.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(world4.getName()).thenReturn("hfhhfhf_nether");
        when(world4.getEnvironment()).thenReturn(World.Environment.NETHER);

        assertTrue(Util.sameWorld(world, world));
        assertTrue(Util.sameWorld(world2, world2));
        assertTrue(Util.sameWorld(world3, world3));
        assertTrue(Util.sameWorld(world, world2));
        assertTrue(Util.sameWorld(world, world3));
        assertTrue(Util.sameWorld(world2, world));
        assertTrue(Util.sameWorld(world2, world3));
        assertTrue(Util.sameWorld(world3, world2));
        assertTrue(Util.sameWorld(world3, world));
        assertFalse(Util.sameWorld(world4, world));
        assertFalse(Util.sameWorld(world4, world2));
        assertFalse(Util.sameWorld(world4, world3));
        assertFalse(Util.sameWorld(world, world4));
        assertFalse(Util.sameWorld(world2, world4));
        assertFalse(Util.sameWorld(world3, world4));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#getWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetWorld() {
        assertNull(Util.getWorld(null));
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.getName()).thenReturn("world_name");
        when(Bukkit.getWorld(eq("world_name"))).thenReturn(world);
        assertEquals(world, Util.getWorld(world));
        // Nether
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(nether.getName()).thenReturn("world_name_nether");
        assertEquals("Nether", world, Util.getWorld(nether));
        // End
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        when(end.getName()).thenReturn("world_name_the_end");
        assertEquals("End", world, Util.getWorld(end));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.util.Util#blockFaceToFloat(org.bukkit.block.BlockFace)}.
     */
    @Test
    public void testBlockFaceToFloat() {
        for (BlockFace bf : BlockFace.values()) {
            float r = Util.blockFaceToFloat(bf);
            switch (bf) {
                case EAST -> assertEquals(90F, r, 0);
                case EAST_NORTH_EAST -> assertEquals(67.5F, r, 0);
                case NORTH_EAST -> assertEquals(45F, r, 0);
                case NORTH_NORTH_EAST -> assertEquals(22.5F, r, 0);
                case NORTH_NORTH_WEST -> assertEquals(337.5F, r, 0);
                case NORTH_WEST -> assertEquals(315F, r, 0);
                case SOUTH -> assertEquals(180F, r, 0);
                case SOUTH_EAST -> assertEquals(135F, r, 0);
                case SOUTH_SOUTH_EAST -> assertEquals(157.5F, r, 0);
                case SOUTH_SOUTH_WEST -> assertEquals(202.5F, r, 0);
                case SOUTH_WEST -> assertEquals(225F, r, 0);
                case WEST -> assertEquals(270F, r, 0);
                case WEST_NORTH_WEST -> assertEquals(292.5F, r, 0);
                case WEST_SOUTH_WEST -> assertEquals(247.5F, r, 0);
                default -> assertEquals(0F, r, 0);
            }
        }
    }

    @Test
    public void testIsIntegerInputNotDigits() {
        assertFalse(Util.isInteger("abdjeodl", false));
        assertFalse(Util.isInteger(" ./;.   .!^", false));
    }

    @Test
    public void testIsIntegerInputEmpty() {
        assertFalse(Util.isInteger("", false));
    }

    @Test
    public void testIsIntegerInputNegativeInteger() {
        assertTrue(Util.isInteger("-2", false));
        assertTrue(Util.isInteger("-2", true));
    }

    @Test
    public void testIsIntegerInputPi() {
        assertFalse(Util.isInteger("3.1415", false));
        assertFalse(Util.isInteger("3.1415", true));
    }

    @Test
    public void testIsIntegerInputOK() {
        assertTrue(Util.isInteger("0", true));
        assertTrue(Util.isInteger("+1", true));
        assertTrue(Util.isInteger("-0", true));
        assertTrue(Util.isInteger("14", true));
    }

    @Test
    public void testIsIntegerInputTrailingDot() {
        assertTrue(Util.isInteger("1.", true));
        assertTrue(Util.isInteger("1.", false));
        assertTrue(Util.isInteger("1.000000", false));
        // assertTrue(Util.isInteger("1.000000", true));
        // For some reason, Integer#parseInt() does not support this...
    }

    /**
     * Test for {@link Util#runCommands(world.bentobox.bentobox.api.user.User, java.util.List, String)}
     */
    @Test
    public void testRunCommandsSudoUserOnlinePerformCommand() {
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand(anyString())).thenReturn(true);
        Util.runCommands(user, Collections.singletonList("[SUDO]help"), "test");
        verify(plugin, never()).logError(anyString());
    }

    /**
     * Test for {@link Util#runCommands(world.bentobox.bentobox.api.user.User, java.util.List, String)}
     */
    @Test
    public void testRunCommandsSudoUserOnlineFailCommand() {
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand(anyString())).thenReturn(false);
        Util.runCommands(user, Collections.singletonList("[SUDO]help"), "test");
        verify(plugin).logError(eq("Could not execute test command for tastybento: help"));
    }

    /**
     * Test for {@link Util#runCommands(world.bentobox.bentobox.api.user.User, java.util.List, String)}
     */
    @Test
    public void testRunCommandsSudoUserOfflineCommand() {
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(false);
        when(user.performCommand(anyString())).thenReturn(true);
        Util.runCommands(user, Collections.singletonList("[SUDO]help"), "test");
        verify(plugin).logError(eq("Could not execute test command for tastybento: help"));
    }

    /**
     * Test for {@link Util#runCommands(world.bentobox.bentobox.api.user.User, java.util.List, String)}
     */
    @Test
    public void testRunCommandsConsoleCommand() {
        when(user.getName()).thenReturn("tastybento");
        when(Bukkit.dispatchCommand(eq(sender), anyString())).thenReturn(true);
        Util.runCommands(user, List.of("replace [player]", "replace owner [owner]", "[owner] [player]"), "test");
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.dispatchCommand(sender, "replace tastybento");
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.dispatchCommand(sender, "replace owner tastybento");
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.dispatchCommand(sender, "tastybento tastybento");
        verify(plugin, never()).logError(anyString());
    }

    /**
     * Test for {@link Util#runCommands(world.bentobox.bentobox.api.user.User, java.util.List, String)}
     */
    @Test
    public void testRunCommandsConsoleCommandFail() {
        when(user.getName()).thenReturn("tastybento");
        when(Bukkit.dispatchCommand(eq(sender), anyString())).thenReturn(false);
        Util.runCommands(user, Collections.singletonList("replace [player]"), "test");
        PowerMockito.verifyStatic(Bukkit.class);
        Bukkit.dispatchCommand(sender, "replace tastybento");
        verify(plugin).logError("Could not execute test command as console: replace tastybento");
    }

    /**
     * Test for {@link Util#broadcast(String, String...)}
     */
    @Test
    public void testBroadcastStringStringNoPlayers() {
        when(Bukkit.getOnlinePlayers()).thenReturn(Collections.emptySet());
        int result = Util.broadcast("test.key", TextVariables.DESCRIPTION, "hello");
        assertEquals(0, result);
    }

    /**
     * Test for {@link Util#broadcast(String, String...)}
     */
    @Test
    public void testBroadcastStringStringHasPerm() {
        int result = Util.broadcast("test.key", TextVariables.DESCRIPTION, "hello");
        assertEquals(11, result);

    }

    /**
     * Test for {@link Util#translateColorCodes(String)}
     */
    @Test
    public void testTranslateColorCodesAmpersand() {
        assertEquals("", Util.translateColorCodes(""));
        assertEquals("abcdef ABCDEF", Util.translateColorCodes("abcdef ABCDEF"));
        assertEquals("white space after   ", Util.translateColorCodes("white space after   "));
        assertEquals("§ared color", Util.translateColorCodes("&a red color"));
        assertEquals("§a   big space", Util.translateColorCodes("&a    big space"));
        assertEquals("§ared color", Util.translateColorCodes("&ared color"));
        assertEquals("§ared §bcolor §cgreen §fheheh", Util.translateColorCodes("&ared &bcolor &c green &f heheh"));
    }

    /**
     * Test for {@link Util#translateColorCodes(String)}
     */
    @Test
    public void testTranslateColorCodesHex() {
        // Use Bungee Chat parsing for single color test to validate correct parsing
        assertEquals(ChatColor.of("#ff0000").toString(), Util.translateColorCodes("&#ff0000"));
        assertEquals(ChatColor.of("#ff2200").toString(), Util.translateColorCodes("&#f20"));

        assertEquals("&#f single char", Util.translateColorCodes("&#f single char"));
        assertEquals("&#f0 two chars", Util.translateColorCodes("&#f0 two chars"));
        assertEquals("§x§f§f§0§0§0§0shorten hex", Util.translateColorCodes("&#f00 shorten hex"));
        assertEquals("§x§f§f§0§0§0§01 four chars", Util.translateColorCodes("&#f001 four chars"));
        assertEquals("§x§f§f§0§0§0§01f five chars", Util.translateColorCodes("&#f001f five chars"));
        assertEquals("§x§f§f§0§0§0§0full hex", Util.translateColorCodes("&#ff0000 full hex"));
        assertEquals("&#ggg outside hex range", Util.translateColorCodes("&#ggg outside hex range"));
    }
}
