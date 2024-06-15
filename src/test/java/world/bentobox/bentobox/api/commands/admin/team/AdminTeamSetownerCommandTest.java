package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
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
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class })
public class AdminTeamSetownerCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid = UUID.randomUUID();
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID = UUID.randomUUID();
    @Mock
    private Island island;
    private AdminTeamSetownerCommand itl;
    @Mock
    private @NonNull Location location;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.getName()).thenReturn("tastybento");
        User.getInstance(p);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        @NonNull
        WorldSettings worldSettings = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // Location
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getCenter()).thenReturn(location);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Plugin Manager
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // DUT
        itl = new AdminTeamSetownerCommand(ac);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "commands.help.console");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#setup()}
     */
    @Test
    public void testSetup() {
        assertEquals("commands.admin.team.setowner.description", itl.getDescription());
        assertEquals("commands.admin.team.setowner.parameters", itl.getParameters());
        assertTrue(itl.isOnlyPlayer());
        assertEquals("mod.team.setowner", itl.getPermission());
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testExecuteMakeOwnerAlreadyOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(uuid);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.team.setowner.already-owner", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        assertTrue(itl.execute(user, itl.getLabel(), List.of("tastybento")));
        // Add other verifications
        verify(user).getTranslation("commands.admin.team.setowner.confirmation", TextVariables.NAME, "tastybento",
                TextVariables.XYZ, "1,2,3");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    public void testChangeOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    public void testChangeOwnerNoOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(null);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }
}
