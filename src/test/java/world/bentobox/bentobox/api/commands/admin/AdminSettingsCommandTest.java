package world.bentobox.bentobox.api.commands.admin;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class})
public class AdminSettingsCommandTest {

    private AdminSettingsCommand asc;
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Player p;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Island island;
    @Mock
    private Location spawnPoint;
    @Mock
    private World world;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
    @Mock
    private PluginManager pluginManager;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);
        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getFlagsDisplayMode(any())).thenReturn(Mode.BASIC);
        //Island Manager
        when(plugin.getIslands()).thenReturn(im);
        // Island - player has island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getUUID(anyString())).thenReturn(uuid);
        when(Util.tabLimit(any(), any())).thenCallRealMethod();

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        ItemMeta bannerMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        Inventory inventory = mock(Inventory.class);
        when(Bukkit.createInventory(eq(null), Mockito.anyInt(), any())).thenReturn(inventory);
        when(Bukkit.createInventory(eq(null), any(InventoryType.class), any())).thenReturn(inventory);
        // Flags manager
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);
        FlagsManager fm = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(fm);

        // RnksManager
        when(plugin.getRanksManager()).thenReturn(new RanksManager());



        asc = new AdminSettingsCommand(ac);

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
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertFalse(asc.isOnlyPlayer());
        assertEquals("admin.settings", asc.getPermission());
        assertEquals("commands.admin.settings.parameters", asc.getParameters());
        assertEquals("commands.admin.settings.description", asc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteEmpty() {
        assertTrue(asc.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgUnknownPlayer() {
        when(Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayerNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayerIslandNotOwner() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgKnownPlayer() {
        assertTrue(asc.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgSpawnNoSpawn() {
        when(Util.getUUID(anyString())).thenReturn(null);
        assertFalse(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "spawn-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArgSpawnSpawn() {
        when(im.getSpawn(any())).thenReturn(Optional.of(island));
        assertTrue(asc.canExecute(user, "", Collections.singletonList("spawn-island")));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsConsole() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(asc.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgs() {
        assertTrue(asc.execute(user, "", Collections.emptyList()));
        verify(pm).setFlagsDisplayMode(user.getUniqueId(), Mode.EXPERT);
        // Open panel
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgs() {
        assertTrue(asc.execute(user, "", Arrays.asList("blah","blah")));
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringTwoArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b","WORLD_TNT"));
        assertFalse(r.isEmpty());
        assertEquals("WORLD_TNT_DAMAGE", r.get().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringThreeArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b","WORLD_TNT", "BEACO"));
        assertFalse(r.isEmpty());
        assertEquals("BEACON", r.get().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSettingsCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringFourArgs() {
        Optional<List<String>> r = asc.tabComplete(user, "", Arrays.asList("b","b", "PVP_OVERWORLD", "t"));
        assertFalse(r.isEmpty());
        // TODO - finish this.
    }

}
