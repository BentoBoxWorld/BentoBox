/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
        when(s.getRankCommand(Mockito.anyString())).thenReturn(RanksManager.OWNER_RANK);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        // Addon
        when(ic.getAddon()).thenReturn(addon);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // No team to start
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        when(island.getRank(Mockito.any())).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(island);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Server and Plugin Manager for events
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        PluginManager pim = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pim);

        when(island.getWorld()).thenReturn(mock(World.class));

        // Locales
        when(lm.get(Mockito.any(User.class), Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Class
        iec = new IslandExpelCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
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
        Mockito.verify(user).sendMessage("commands.help.header", "[label]", null);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteTooManyArgs() {
        assertFalse(iec.canExecute(user, "", Arrays.asList("Hello", "there")));
        Mockito.verify(user).sendMessage("commands.help.header", "[label]", null);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTeamNoIsland() {

        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownTargetUserInTeam() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tasty");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownTargetUserHasIsland() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tasty");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(island.getRank(Mockito.any())).thenReturn(RanksManager.VISITOR_RANK);
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.errors.no-permission");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSelf() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.singleton(uuid));
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("commands.island.expel.cannot-expel-yourself");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteTeamMember() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.singleton(target));
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("commands.island.expel.cannot-expel-member");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOfflinePlayer() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.errors.offline-player");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNotOnIsland() {
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        Player p = mock(Player.class);
        when(p.isOnline()).thenReturn(true);
        when(p.getUniqueId()).thenReturn(target);
        User.getInstance(p);
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("commands.island.expel.not-on-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOp() {
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        Player p = mock(Player.class);
        when(p.isOnline()).thenReturn(true);
        when(p.getUniqueId()).thenReturn(target);
        when(p.isOp()).thenReturn(true);
        User.getInstance(p);
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteBypassPerm() {
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        UUID target = UUID.randomUUID();
        Player p = mock(Player.class);
        when(p.isOnline()).thenReturn(true);
        when(p.getUniqueId()).thenReturn(target);
        when(p.hasPermission(Mockito.anyString())).thenReturn(true);
        User.getInstance(p);
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.emptySet());
        assertFalse(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        when(im.locationIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        setUpTarget();
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(Collections.emptySet());
        assertTrue(iec.canExecute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user, Mockito.never()).sendMessage(Mockito.anyString());
    }

    private Player setUpTarget() {
        UUID target = UUID.randomUUID();
        Player p = mock(Player.class);
        when(p.isOnline()).thenReturn(true);
        when(p.getUniqueId()).thenReturn(target);
        when(p.getLocation()).thenReturn(mock(Location.class));
        when(p.performCommand(Mockito.anyString())).thenReturn(true);
        when(p.getName()).thenReturn("target");
        User.getInstance(p);
        when(pm.getUUID(Mockito.anyString())).thenReturn(target);
        return p;
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHasIsland() {
        testCanExecute();
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslandSendToSpawn() {
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getSpawn(Mockito.any())).thenReturn(optionalIsland);
        testCanExecute();
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(im).spawnTeleport(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCreateIsland() {
        GameModeAddon gma = mock(GameModeAddon.class);
        CompositeCommand pc = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalPlayerCommand = Optional.of(pc);
        when(pc.getSubCommand(Mockito.anyString())).thenReturn(optionalPlayerCommand);
        when(gma.getPlayerCommand()).thenReturn(optionalPlayerCommand);
        Optional<GameModeAddon> optionalAddon = Optional.of(gma);
        when(iwm.getAddon(Mockito.any())).thenReturn(optionalAddon);
        when(im.getSpawn(Mockito.any())).thenReturn(Optional.empty());
        testCanExecute();
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        assertTrue(iec.execute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(addon).logWarning(Mockito.eq("Expel: target had no island, so one was created"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandExpelCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCreateIslandFailCommand() {
        GameModeAddon gma = mock(GameModeAddon.class);
        CompositeCommand pc = mock(CompositeCommand.class);
        Optional<CompositeCommand> optionalPlayerCommand = Optional.empty();
        when(pc.getSubCommand(Mockito.anyString())).thenReturn(optionalPlayerCommand);
        when(gma.getPlayerCommand()).thenReturn(optionalPlayerCommand);
        Optional<GameModeAddon> optionalAddon = Optional.of(gma);
        when(iwm.getAddon(Mockito.any())).thenReturn(optionalAddon);
        when(im.getSpawn(Mockito.any())).thenReturn(Optional.empty());
        testCanExecute();
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        assertFalse(iec.execute(user, "", Collections.singletonList("tasty")));
        Mockito.verify(addon).logError(Mockito.eq("Expel: target had no island, and one could not be created"));
        Mockito.verify(user).sendMessage("commands.island.expel.cannot-expel");
    }

}
