package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.Nullable;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamSetownerCommandTest {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Player player;
    @Mock
    private CompositeCommand subCommand;
    @Mock
    private PlayersManager pm;
    @Mock
    private World world;
    private IslandTeamSetownerCommand its;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.getName()).thenReturn("tastybento");

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        Optional<CompositeCommand> optionalCommand = Optional.of(subCommand);
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(optionalCommand);
        when(ic.getWorld()).thenReturn(world);

        // Player has island to begin with
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(any(), any())).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island World Manager
        when(plugin.getIWM()).thenReturn(iwm);

        // Plugin Manager
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Island
        Island island = mock(Island.class);
        when(island.getUniqueId()).thenReturn("uniqueid");
        when(im.getIsland(any(), Mockito.any(User.class))).thenReturn(island);

        // Class under test
        its = new IslandTeamSetownerCommand(ic);
    }

    /**
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("island.team.setowner", its.getPermission());
        assertTrue(its.isOnlyPlayer());
        assertEquals("commands.island.team.setowner.parameters", its.getParameters());
        assertEquals("commands.island.team.setowner.description", its.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNullOwner() {
        when(im.getOwner(any(), any())).thenReturn(null);
        assertFalse(its.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.not-owner");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNotInTeam() {
        when(im.inTeam(any(), any())).thenReturn(false);
        assertFalse(its.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-team");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNotOwner() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(UUID.randomUUID());
        assertFalse(its.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.not-owner");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringShowHelp() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        assertFalse(its.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.help.header","[label]", null);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownPlayer() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        when(pm.getUUID(anyString())).thenReturn(null);
        assertFalse(its.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSamePlayer() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        when(pm.getUUID(anyString())).thenReturn(uuid);
        assertFalse(its.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringTargetNotInTeam() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        when(pm.getUUID(anyString())).thenReturn(UUID.randomUUID());
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(uuid));
        assertFalse(its.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.island.team.setowner.errors.target-is-not-member");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSuccess() {
        when(im.inTeam(any(), any())).thenReturn(true);
        when(im.getOwner(any(), any())).thenReturn(uuid);
        UUID target = UUID.randomUUID();
        when(pm.getUUID(anyString())).thenReturn(target);
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(target));
        @Nullable
        Island island = mock(Island.class);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);

        assertTrue(its.execute(user, "", Collections.singletonList("tastybento")));
        verify(im).setOwner(any(), eq(user), eq(target));
        verify(im).save(island);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        assertTrue(its.tabComplete(user, "", Collections.emptyList()).get().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringUnknown() {
        assertTrue(its.tabComplete(user, "ta", Collections.emptyList()).get().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamSetownerCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringMember() {
        UUID target = UUID.randomUUID();
        when(pm.getName(any())).thenReturn("tastybento");
        when(im.getMembers(any(), any())).thenReturn(Collections.singleton(target));
        assertEquals("tastybento", its.tabComplete(user, "", Collections.emptyList()).get().get(0));
    }

}
