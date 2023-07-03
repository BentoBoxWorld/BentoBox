package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandDeletehomeCommandTest {

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
    private IslandDeletehomeCommand idh;
    @Mock
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);
        when(plugin.getIslands()).thenReturn(im);
        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.onIsland(any())).thenReturn(true);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        @NotNull
        Map<String, Location> homeMap = new HashMap<>();
        homeMap.put("Home", null);
        homeMap.put("Home2", null);
        homeMap.put("Home3", null);
        homeMap.put("Home4", null);
        when(island.getHomes()).thenReturn(homeMap);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(any())).thenReturn(3);
        when(plugin.getIWM()).thenReturn(iwm);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        idh = new IslandDeletehomeCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#IslandDeletehomeCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandDeletehomeCommand() {
        assertEquals("deletehome", idh.getLabel());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(idh.isOnlyPlayer());
        assertEquals("commands.island.deletehome.parameters", idh.getParameters());
        assertEquals("commands.island.deletehome.description", idh.getDescription());
        assertEquals("permission.island.deletehome", idh.getPermission());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteHelp() {
        idh.canExecute(user, "label", List.of());
        verify(user).sendMessage("commands.help.header","[label]","commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.getIsland(any(), eq(user))).thenReturn(null);
        assertFalse(idh.canExecute(user, "label", List.of("something")));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(user)).thenReturn(RanksManager.COOP_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(idh.canExecute(user, "label", List.of("something")));
        verify(user).sendMessage("general.errors.insufficient-rank",
                TextVariables.RANK, "ranks.coop");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownHome() {
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.COOP_RANK);
        when(island.getHomes()).thenReturn(Map.of("home", mock(Location.class)));

        when(im.isHomeLocation(eq(island), anyString())).thenReturn(false);

        assertFalse(idh.canExecute(user, "label", List.of("something")));
        verify(user).sendMessage("commands.island.go.unknown-home");
        verify(user).sendMessage("commands.island.sethome.homes-are");
        verify(user).sendMessage("home-list-syntax", TextVariables.NAME, "home");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownHome() {
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.COOP_RANK);
        when(island.getHomes()).thenReturn(Map.of("home", mock(Location.class)));

        when(im.isHomeLocation(eq(island), anyString())).thenReturn(true);

        assertTrue(idh.canExecute(user, "label", List.of("home")));
    }
    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertTrue(idh.execute(user, "label", List.of("home")));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "10");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        when(island.getHomes()).thenReturn(Map.of("home", mock(Location.class)));
        Optional<List<String>> list = idh.tabComplete(user, "label", List.of("hom"));
        assertTrue(list.isPresent());
        assertEquals("home", list.get().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandDeletehomeCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringNothing() {
        when(island.getHomes()).thenReturn(Map.of("home", mock(Location.class)));
        Optional<List<String>> list = idh.tabComplete(user, "label", List.of("f"));
        assertTrue(list.isPresent());
        assertTrue(list.get().isEmpty());
    }

}
