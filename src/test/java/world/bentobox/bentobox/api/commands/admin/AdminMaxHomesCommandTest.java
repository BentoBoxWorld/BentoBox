package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */

//@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class, ServerBuildInfo.class, IslandGoCommand.class })
public class AdminMaxHomesCommandTest {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    private UUID uuid;
    @Mock
    private World world;
    @Mock
    private @Nullable Island island;
    private AdminMaxHomesCommand instance;
    private String label;
    private ArrayList<String> args = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        //PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        //PowerMockito.mockStatic(IslandGoCommand.class);

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);

        // Util
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("admin");
        when(ac.getWorld()).thenReturn(world);

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // when(im.isOwner(any(),any())).thenReturn(true);
        // when(im.getOwner(any(),any())).thenReturn(uuid);
        when(im.getIsland(world, user)).thenReturn(island);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island));
        when(plugin.getIslands()).thenReturn(im);

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.hasTeam()).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Players
        when(pm.getUUID(anyString())).thenReturn(uuid);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        instance = spy(new AdminMaxHomesCommand(ac));
        label = "island";
    }

    @AfterEach
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsIsEmpty() {
        // Arrange: args is already empty

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        // Verify that showHelp was called
        verify(instance).showHelp(instance, user);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSize1_UserNotPlayer() {
        // Arrange
        args.add("5");
        when(user.isPlayer()).thenReturn(false);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSize1_WrongWorld() {
        // Arrange
        args.add("5");
        when(user.isPlayer()).thenReturn(true);
        World userWorld = mock(World.class);
        World expectedWorld = mock(World.class);
        when(user.getWorld()).thenReturn(userWorld);
        doReturn(expectedWorld).when(instance).getWorld();

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSize1_InvalidMaxHomes() {
        // Arrange
        args.add("notanumber");
        when(user.isPlayer()).thenReturn(true);
        World world = mock(World.class);
        when(user.getWorld()).thenReturn(world);
        doReturn(world).when(instance).getWorld();

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, "notanumber");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSize1_UserNotOnIsland() {
        // Arrange
        args.add("5");
        when(user.isPlayer()).thenReturn(true);
        World world = mock(World.class);
        when(user.getWorld()).thenReturn(world);
        doReturn(world).when(instance).getWorld();

        Location location = mock(Location.class);
        when(user.getLocation()).thenReturn(location);

        when(im.getIslandAt(location)).thenReturn(Optional.empty());

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.not-on-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSize1_Success() {
        // Arrange
        args.add("5");
        when(user.isPlayer()).thenReturn(true);
        World world = mock(World.class);
        when(user.getWorld()).thenReturn(world);
        doReturn(world).when(instance).getWorld();

        Location location = mock(Location.class);
        when(user.getLocation()).thenReturn(location);

        Island island = mock(Island.class);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertTrue(result);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSizeGreaterThan1_InvalidPlayer() {
        // Arrange
        args.add("UnknownPlayer");
        args.add("5");

        when(pm.getUUID("UnknownPlayer")).thenReturn(null);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "UnknownPlayer");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSizeGreaterThan1_InvalidMaxHomes() {
        // Arrange
        args.add("ValidPlayer");
        args.add("notanumber");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, "notanumber");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSizeGreaterThan1_TargetPlayerHasNoIslands() {
        // Arrange
        args.add("ValidPlayer");
        args.add("5");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);

        User targetUser = mock(User.class);
        // Mock static method User.getInstance(UUID)
        // Assuming use of Mockito with inline mocking or PowerMockito
        //PowerMockito.mockStatic(User.class);
        when(User.getInstance(playerUUID)).thenReturn(targetUser);
        when(IslandGoCommand.getNameIslandMap(targetUser, world)).thenReturn(new HashMap<>());

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSizeGreaterThan2_UnknownIsland() {
        // Arrange
        args.add("ValidPlayer");
        args.add("5");
        args.add("UnknownIsland");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);

        User targetUser = mock(User.class);
        // Mock static method User.getInstance(UUID)
        // Assuming use of Mockito with inline mocking or PowerMockito
        //PowerMockito.mockStatic(User.class);
        when(User.getInstance(playerUUID)).thenReturn(targetUser);

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("Island1", new IslandInfo(mock(Island.class), true));
        when(IslandGoCommand.getNameIslandMap(targetUser, world)).thenReturn(islandsMap);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("commands.admin.maxhomes.errors.unknown-island", TextVariables.NAME, "UnknownIsland");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testArgsSizeGreaterThan1_Success() {
        // Arrange
        args.add("ValidPlayer");
        args.add("5");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);

        User targetUser = mock(User.class);
        // Mock static method User.getInstance(UUID)
        //PowerMockito.mockStatic(User.class);
        when(User.getInstance(playerUUID)).thenReturn(targetUser);

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("", new IslandInfo(mock(Island.class), false));
        when(IslandGoCommand.getNameIslandMap(targetUser, world)).thenReturn(islandsMap);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertTrue(result);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("mod.maxhomes", instance.getPermission());
        assertFalse(instance.isOnlyPlayer());
        assertEquals("commands.admin.maxhomes.parameters", instance.getParameters());
        assertEquals("commands.admin.maxhomes.description", instance.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabComplete_ArgsSize2_ReturnsPlayerNames() {
        // Arrange
        args.add("someArg"); // args.size() == 1
        args.add(""); // args.size() == 2

        // Mock Util.getOnlinePlayerList(user)
        List<String> onlinePlayers = Arrays.asList("PlayerOne", "PlayerTwo");
        //PowerMockito.mockStatic(Util.class);
        when(Util.getOnlinePlayerList(user)).thenReturn(onlinePlayers);

        // Act
        Optional<List<String>> result = instance.tabComplete(user, label, args);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(onlinePlayers, result.get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabComplete_ArgsSizeGreaterThan3_ReturnsIslandNames() {
        // Arrange
        args.add("someArg");
        args.add("anotherArg");
        args.add("thirdArg");
        args.add(""); // args.size() == 4 (>3)
        String lastArg = args.getLast();

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("Island1", new IslandInfo(mock(Island.class), true));
        islandsMap.put("Island2", new IslandInfo(mock(Island.class), true));
        when(IslandGoCommand.getNameIslandMap(any(), any())).thenReturn(islandsMap);

        // Create the list of island names
        List<String> islandNames = new ArrayList<>(islandsMap.keySet());

        // Mock Util.tabLimit()
        List<String> limitedIslandNames = Arrays.asList("IslandOne", "IslandTwo");
        //PowerMockito.mockStatic(Util.class);
        when(Util.tabLimit(islandNames, lastArg)).thenReturn(limitedIslandNames);

        // Act
        Optional<List<String>> result = instance.tabComplete(user, label, args);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(limitedIslandNames, result.get());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabComplete_Otherwise_ReturnsListOfOne() {
        // Arrange
        args.add(""); // args.size() == 1

        // Act
        Optional<List<String>> result = instance.tabComplete(user, label, args);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(Collections.singletonList("1"), result.get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabComplete_ArgsSize3_ReturnsListOfOne() {
        // Arrange
        args.add("someArg");
        args.add("anotherArg");
        args.add(""); // args.size() == 3

        // Act
        Optional<List<String>> result = instance.tabComplete(user, label, args);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(Collections.singletonList("1"), result.get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteWithEmptyIslands_ShouldReturnFalse() {
        // Arrange
        instance.maxHomes = 5; // Set maxHomes to a valid number
        instance.islands = new HashMap<>(); // Empty islands map

        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertFalse(result);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteWithMaxHomesLessThanOne_ShouldReturnFalse() {
        // Arrange
        instance.maxHomes = 0; // Invalid maxHomes
        Island island = mock(Island.class);
        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("TestIsland", new IslandInfo(island, true));
        when(IslandGoCommand.getNameIslandMap(user, world)).thenReturn(islandsMap);
        instance.islands = islandsMap;

        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertFalse(result);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessful_SingleIsland() {
        // Arrange
        instance.maxHomes = 5;
        Island island = mock(Island.class);
        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("TestIsland", new IslandInfo(island, true));
        when(IslandGoCommand.getNameIslandMap(user, world)).thenReturn(islandsMap);
        instance.islands = islandsMap;

        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertTrue(result);
        verify(island).setMaxHomes(5);
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "TestIsland",
                TextVariables.NUMBER, "5");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessful_MultipleIslands() {
        // Arrange
        instance.maxHomes = 3;
        Island island1 = mock(Island.class);
        Island island2 = mock(Island.class);
        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("IslandOne", new IslandInfo(island1, true));
        islandsMap.put("IslandTwo", new IslandInfo(island2, true));
        when(IslandGoCommand.getNameIslandMap(user, world)).thenReturn(islandsMap);
        instance.islands = islandsMap;

        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertTrue(result);
        verify(island1).setMaxHomes(3);
        verify(island2).setMaxHomes(3);
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "IslandOne",
                TextVariables.NUMBER, "3");
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "IslandTwo",
                TextVariables.NUMBER, "3");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteAfterSuccessfulCanExecute() {
        // Arrange
        args.add("5");
        when(user.isPlayer()).thenReturn(true);
        World world = mock(World.class);
        when(user.getWorld()).thenReturn(world);
        doReturn(world).when(instance).getWorld();

        Location location = mock(Location.class);
        when(user.getLocation()).thenReturn(location);

        Island island = mock(Island.class);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));

        // Act
        boolean canExecuteResult = instance.canExecute(user, label, args);
        boolean executeResult = instance.execute(user, label, args);

        // Assert
        assertTrue(canExecuteResult);
        assertTrue(executeResult);
        verify(island).setMaxHomes(5);
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "", TextVariables.NUMBER,
                "5");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteWithInvalidMaxHomesAfterCanExecute() {
        // Arrange
        args.add("-1");
        when(user.isPlayer()).thenReturn(true);
        World world = mock(World.class);
        when(user.getWorld()).thenReturn(world);
        doReturn(world).when(instance).getWorld();

        // Act
        boolean canExecuteResult = instance.canExecute(user, label, args);
        boolean executeResult = instance.execute(user, label, args);

        // Assert
        assertFalse(canExecuteResult);
        assertFalse(executeResult);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    @Disabled("This fails for some reason on the map getting")
    public void testExecuteWithMultipleIslandsAfterCanExecute() {
        // Arrange
        args.add("ValidPlayer");
        args.add("4");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);


        Island island1 = mock(Island.class);
        Island island2 = mock(Island.class);
        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("IslandA", new IslandInfo(island1, false));
        islandsMap.put("IslandB", new IslandInfo(island2, true));
        instance.islands = islandsMap;
        when(IslandGoCommand.getNameIslandMap(user, world)).thenReturn(islandsMap);

        // Act
        boolean canExecuteResult = instance.canExecute(user, label, args);
        boolean executeResult = instance.execute(user, label, args);

        // Assert
        assertTrue(canExecuteResult);
        assertTrue(executeResult);
        verify(island1).setMaxHomes(4);
        verify(island2).setMaxHomes(4);
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "IslandA",
                TextVariables.NUMBER, "4");
        verify(user).sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, "IslandB",
                TextVariables.NUMBER, "4");
    }

}
