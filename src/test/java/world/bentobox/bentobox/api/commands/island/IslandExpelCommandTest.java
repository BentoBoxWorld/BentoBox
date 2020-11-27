package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
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
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandExpelCommandTest {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;
    @Mock
    private LocalesManager lm;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Addon addon;

    private IslandExpelCommand iec;
    @Mock
    private Server server;
    @Mock
    private Player p;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        // Sometimes use Mockito.withSettings().verboseLogging()
        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptySet());
        when(p.getServer()).thenReturn(server);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        // Addon
        when(ic.getAddon()).thenReturn(addon);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // No team to start
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Server and Plugin Manager for events
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        when(island.getWorld()).thenReturn(mock(World.class));

        // Locales
        Answer<String> answer = invocation -> invocation.getArgument(1, String.class);
        when(lm.get(any(User.class), anyString())).thenAnswer(answer);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

        // Class
        iec = new IslandExpelCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#IslandExpelCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandExpelCommand() {
        assertEquals("expel", iec.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(iec.isOnlyPlayer());
        assertEquals("bskyblock.island.expel", iec.getPermission());
        assertEquals("commands.island.expel.parameters", iec.getParameters());
        assertEquals("commands.island.expel.description", iec.getDescription());
        assertTrue(iec.isConfigurableRankCommand());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoArgs() {
        assertFalse(iec.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.help.header", "[label]", "commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteTooManyArgs() {
        assertFalse(iec.canExecute(user, "", Arrays.asList("Hello", "there")));
        verify(user).sendMessage("commands.help.header", "[label]", "commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTeamNoIsland() {

        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownTargetUserInTeam() {
        when(im.inTeam(any(), any())).thenReturn(true);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tasty");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownTargetUserHasIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tasty");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.VISITOR_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.visitor"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSelf() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(pm.getUUID(anyString())).thenReturn(uuid);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(uuid));
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.cannot-expel-yourself");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteTeamMember() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(target);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(target));
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.cannot-expel-member");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOfflinePlayer() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(target);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("general.errors.offline-player");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteInvisiblePlayer() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        Player t = setUpTarget();
        when(p.canSee(t)).thenReturn(false);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("general.errors.offline-player");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNotOnIsland() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        setUpTarget();
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.not-on-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOp() {
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        Player t = setUpTarget();
        when(t.isOp()).thenReturn(true);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteBypassPerm() {
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        Player t = setUpTarget();
        when(t.hasPermission(anyString())).thenReturn(true);
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        setUpTarget();
        when(im.getMembers(any(), any())).thenReturn(Collections.emptySet());
        assertTrue(iec.canExecute(user, "", Collections.singletonList("tasty")));
        verify(user, never()).sendMessage(anyString());
    }

    private Player setUpTarget() {
        UUID target = UUID.randomUUID();
        Player t = mock(Player.class);
        when(t.isOnline()).thenReturn(true);
        when(t.getUniqueId()).thenReturn(target);
        when(t.getLocation()).thenReturn(mock(Location.class));
        when(t.performCommand(anyString())).thenReturn(true);
        when(t.getName()).thenReturn("target");
        when(t.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptySet());
        User.getInstance(t);
        when(pm.getUUID(anyString())).thenReturn(target);
        when(p.canSee(t)).thenReturn(true);
        when(Bukkit.getPlayer(target)).thenReturn(t);
        return t;
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHasIsland() {
        testCanExecute();
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.success", TextVariables.NAME, "target");
        verify(im).homeTeleportAsync(any(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandSendToSpawn() {
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getSpawn(any())).thenReturn(optionalIsland);
        testCanExecute();
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.success", TextVariables.NAME, "target");
        verify(im).spawnTeleport(any(), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCreateIsland() {
        GameModeAddon gma = mock(GameModeAddon.class);
        CompositeCommand pc = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalPlayerCommand = Optional.of(pc);
        when(pc.getSubCommand(anyString())).thenReturn(optionalPlayerCommand);
        when(gma.getPlayerCommand()).thenReturn(optionalPlayerCommand);
        Optional<GameModeAddon> optionalAddon = Optional.of(gma);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(im.getSpawn(any())).thenReturn(Optional.empty());
        testCanExecute();
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        verify(user).sendMessage("commands.island.expel.success", TextVariables.NAME, "target");
        verify(addon).logWarning(eq("Expel: target had no island, so one was created"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCreateIslandFailCommand() {
        GameModeAddon gma = mock(GameModeAddon.class);
        CompositeCommand pc = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalPlayerCommand = Optional.empty();
        when(pc.getSubCommand(anyString())).thenReturn(optionalPlayerCommand);
        when(gma.getPlayerCommand()).thenReturn(optionalPlayerCommand);
        Optional<GameModeAddon> optionalAddon = Optional.of(gma);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(im.getSpawn(any())).thenReturn(Optional.empty());
        testCanExecute();
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);
        assertFalse(iec.execute(user, "", Collections.singletonList("tasty")));
        verify(addon).logError(eq("Expel: target had no island, and one could not be created"));
        verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#tabComplete(User, String, java.util.List)}
     */
    @Test
    public void testTabCompleteUserStringListNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        assertFalse(iec.tabComplete(user, "", Collections.emptyList()).isPresent());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#tabComplete(User, String, java.util.List)}
     */
    @Test
    public void testTabCompleteUserStringListNoPlayersOnIsland() {
        assertTrue(iec.tabComplete(user, "", Collections.emptyList()).get().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#tabComplete(User, String, java.util.List)}
     */
    @Test
    public void testTabCompleteUserStringListPlayersOnIsland() {
        List<Player> list = new ArrayList<>();
        Player p1 = mock(Player.class);
        when(p1.getName()).thenReturn("normal");
        when(p.canSee(p1)).thenReturn(true);
        Player p2 = mock(Player.class);
        when(p2.getName()).thenReturn("op");
        when(p.canSee(p2)).thenReturn(true);
        when(p2.isOp()).thenReturn(true);
        Player p3 = mock(Player.class);
        when(p3.getName()).thenReturn("invisible");
        Player p4 = mock(Player.class);
        when(p4.getName()).thenReturn("adminPerm");
        when(p.canSee(p4)).thenReturn(true);
        when(p4.hasPermission(eq("bskyblock.admin.noexpel"))).thenReturn(true);
        Player p5 = mock(Player.class);
        when(p5.getName()).thenReturn("modPerm");
        when(p.canSee(p5)).thenReturn(true);
        when(p5.hasPermission(eq("bskyblock.mod.bypassexpel"))).thenReturn(true);
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p1);
        when(island.getPlayersOnIsland()).thenReturn(list);
        List<String> result = iec.tabComplete(user, "", Collections.emptyList()).get();
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertEquals("normal", result.get(0));
        assertEquals("normal", result.get(1));
    }
}
