package world.bentobox.bentobox.api.commands.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class AdminResetHomeCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    private UUID uuid;
    private AdminResetHomeCommand instance;
    private String label;
    private ArrayList<String> args = new ArrayList<>();
    private MockedStatic<IslandGoCommand> mockedIslandGo;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

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
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("admin");
        when(ac.getWorld()).thenReturn(world);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.getIsland(world, user)).thenReturn(island);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island));

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
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        
        // IslandGoComment
        mockedIslandGo = Mockito.mockStatic(IslandGoCommand.class);

        instance = spy(new AdminResetHomeCommand(ac));
        label = "island";
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
    public void testArgsSizeGreaterThan1_InvalidPlayer() {
        // Arrange
        args.add("UnknownPlayer");

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
    public void testArgsSizeGreaterThan2_UnknownIsland() {
        // Arrange
        args.add("ValidPlayer");
        args.add("UnknownIsland");

        UUID playerUUID = UUID.randomUUID();
        when(pm.getUUID("ValidPlayer")).thenReturn(playerUUID);

        User targetUser = mock(User.class);
        // Mock static method User.getInstance(UUID)
        // Assuming use of Mockito with inline mocking or PowerMockito
        MockedStatic<User> mockUser = Mockito.mockStatic(User.class);
        mockUser.when(() -> User.getInstance(playerUUID)).thenReturn(targetUser);

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("Island1", new IslandInfo(mock(Island.class), false));
        
        mockedIslandGo.when(() -> IslandGoCommand.getNameIslandMap(targetUser, world)).thenReturn(islandsMap);

        // Act
        boolean result = instance.canExecute(user, label, args);

        // Assert
        assertFalse(result);
        verify(user).sendMessage("commands.admin.maxhomes.errors.unknown-island", TextVariables.NAME, "UnknownIsland");
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("mod.resethome", instance.getPermission());
        assertFalse(instance.isOnlyPlayer());
        assertEquals("commands.admin.resethome.parameters", instance.getParameters());
        assertEquals("commands.admin.resethome.description", instance.getDescription());

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
        mockedUtil.when(() -> Util.getOnlinePlayerList(user)).thenReturn(onlinePlayers);

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
    public void testTabComplete_ArgsSizeGreaterThan2_ReturnsIslandNames() {
        // Arrange
        args.add("someArg");
        args.add("anotherArg");
        args.add(""); // args.size() == 3 (>2)
        String lastArg = args.getLast();

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("IslandOne", new IslandInfo(mock(Island.class), false));
        islandsMap.put("IslandTwo", new IslandInfo(mock(Island.class), false));
        
        mockedIslandGo.when(() -> IslandGoCommand.getNameIslandMap(any(), any())).thenReturn(islandsMap);

        // Create the list of island names
        List<String> islandNames = new ArrayList<>(islandsMap.keySet());

        // Mock Util.tabLimit()
        List<String> limitedIslandNames = Arrays.asList("IslandOne", "IslandTwo");
        mockedUtil.when(() -> Util.tabLimit(islandNames, lastArg)).thenReturn(limitedIslandNames);

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
    public void testExecuteWithEmptyIslands_ShouldReturnFalse() {
        // Arrange
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
    public void testExecuteSuccessful_SingleIsland() {

        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("TestIsland", new IslandInfo(island, false));
        mockedIslandGo.when(() -> IslandGoCommand.getNameIslandMap(user, world)).thenReturn(islandsMap);
        instance.islands = islandsMap;
        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertTrue(result);
        verify(island).getHomes();
        verify(user).sendMessage("commands.admin.resethome.cleared", TextVariables.NAME, "TestIsland");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminMaxHomesCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessful_MultipleIslands() {
        // Arrange
        Island island1 = mock(Island.class);
        Island island2 = mock(Island.class);
        Map<String, IslandInfo> islandsMap = new HashMap<>();
        islandsMap.put("IslandOne", new IslandInfo(island1, false));
        islandsMap.put("IslandTwo", new IslandInfo(island2, false));
        mockedIslandGo.when(() -> IslandGoCommand.getNameIslandMap(any(), any())).thenReturn(islandsMap);
        instance.islands = islandsMap;

        // Act
        boolean result = instance.execute(user, label, args);

        // Assert
        assertTrue(result);
        verify(island1).getHomes();
        verify(island2).getHomes();
        verify(user).sendMessage("commands.admin.resethome.cleared", TextVariables.NAME, "IslandOne");
        verify(user).sendMessage("commands.admin.resethome.cleared", TextVariables.NAME, "IslandTwo");
    }

}
