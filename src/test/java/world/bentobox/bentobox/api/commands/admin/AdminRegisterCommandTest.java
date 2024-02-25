package world.bentobox.bentobox.api.commands.admin;

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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
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
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
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
@PrepareForTest({ Bukkit.class, BentoBox.class, Util.class })
public class AdminRegisterCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island is;
    @Mock
    private Location loc;

    private UUID notUUID;

    private IslandDeletionManager idm;
    private AdminRegisterCommand itl;
    @Mock
    private World world;
    @Mock
    private Block block;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        // World
        when(ac.getWorld()).thenReturn(world);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

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
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.getInstance(p);
        User.setPlugin(plugin);

        // Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // when(im.isOwner(any(),any())).thenReturn(true);
        // when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Deletion Manager
        idm = mock(IslandDeletionManager.class);
        when(idm.inDeletion(any())).thenReturn(false);
        when(plugin.getIslandDeletionManager()).thenReturn(idm);

        // Plugin Manager
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Island
        when(is.getWorld()).thenReturn(world);
        when(is.getCenter()).thenReturn(loc);
        when(im.createIsland(any(), eq(uuid))).thenReturn(is);
        when(loc.getBlock()).thenReturn(block);

        // DUT
        itl = new AdminRegisterCommand(ac);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteWrongWorld() {
        when(user.getWorld()).thenReturn(mock(World.class));
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento2")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento2");
    }

    /**
     * Test method for
     * {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).getTranslation("commands.admin.register.no-island-here");
    }


    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteAlreadyOwnedIsland() {
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(1, 2, 3));
        // Island has owner
        when(is.getOwner()).thenReturn(uuid);
        when(is.isOwned()).thenReturn(true);
        when(is.getCenter()).thenReturn(loc);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.register.already-owned");
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteInDeletionIsland() {
        when(idm.inDeletion(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        when(is.getOwner()).thenReturn(uuid);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.register.in-deletion");
    }

    /**
     * Test method for {@link AdminRegisterCommand#canExecute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testCanExecuteSuccess() {
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        when(pm.getUUID(any())).thenReturn(notUUID);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
    }

    /**
     * Test method for {@link AdminRegisterCommand#register(User, String)}.
     */
    @Test
    public void testRegister() {
        testCanExecuteSuccess();
        when(is.isSpawn()).thenReturn(true);
        itl.register(user, "tastybento");
        verify(im).setOwner(user, uuid, is, RanksManager.VISITOR_RANK);
        verify(im).clearSpawn(world);
        verify(user).sendMessage("commands.admin.register.registered-island", TextVariables.XYZ, "", TextVariables.NAME,
                "tastybento");
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    public void testReserveCannotMakeIsland() {
        when(im.createIsland(any(), eq(uuid))).thenReturn(null);
        testCanExecuteNoIsland();
        itl.reserve(user, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user).sendMessage("commands.admin.register.cannot-make-island");
    }

    /**
     * Test method for {@link AdminRegisterCommand#reserve(User, String)}.
     */
    @Test
    public void testReserveCanMakeIsland() {
        testCanExecuteNoIsland();
        itl.reserve(user, "tastybento");
        verify(im).createIsland(any(), eq(uuid));
        verify(user, never()).sendMessage("commands.admin.register.cannot-make-island");
        verify(block).setType(Material.BEDROCK);
        verify(user).sendMessage("commands.admin.register.reserved-island", TextVariables.XYZ, "", TextVariables.NAME,
                "tastybento");
    }

}
