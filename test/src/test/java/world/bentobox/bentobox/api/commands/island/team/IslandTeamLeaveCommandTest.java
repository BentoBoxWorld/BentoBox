package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
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
public class IslandTeamLeaveCommandTest {

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


    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoTeam() {
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(user).sendMessage(eq("general.errors.no-team"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteInOwner() {
        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(user).sendMessage(eq("commands.island.team.leave.cannot-leave"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteNoConfirmation() {
        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Add a team owner - null
        when(im.getOwner(any(), any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(im).setLeaveTeam(any(), eq(uuid));
        verify(user).sendMessage(eq("commands.island.team.leave.success"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteWithConfirmation() {
        when(s.isLeaveConfirmation()).thenReturn(true);
        // 3 second timeout
        when(s.getConfirmationTime()).thenReturn(3);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Add a team owner - null
        when(im.getOwner(any(), any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Confirmation required
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("3"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteWithLoseResetCheckNoResets() {
        // Leaves lose resets
        when(iwm.isLeaversLoseReset(any())).thenReturn(true);

        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Add a team owner - null
        when(im.getOwner(any(), any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(user).sendMessage("commands.island.reset.none-left");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteWithLoseResetCheckHasResets() {
        // Leaves lose resets
        when(iwm.isLeaversLoseReset(any())).thenReturn(true);
        when(pm.getResetsLeft(any(),any(UUID.class))).thenReturn(100);

        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Add a team owner - null
        when(im.getOwner(any(), any())).thenReturn(null);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(im).setLeaveTeam(any(), eq(uuid));
        verify(user).sendMessage(eq("commands.island.team.leave.success"));
        verify(pm).addReset(eq(world), eq(uuid));
        verify(user).sendMessage(eq("commands.island.reset.resets-left"), eq(TextVariables.NUMBER), eq("100"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteTestResets() {
        when(s.isLeaveConfirmation()).thenReturn(false);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Add a team owner - null
        when(im.getOwner(any(), any())).thenReturn(null);

        // Require resets
        when(iwm.isOnLeaveResetEnderChest(any())).thenReturn(true);
        Inventory enderChest = mock(Inventory.class);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(iwm.isOnLeaveResetInventory(any())).thenReturn(true);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(iwm.isOnLeaveResetMoney(any())).thenReturn(true);

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(im).setLeaveTeam(any(), eq(uuid));
        verify(user).sendMessage(eq("commands.island.team.leave.success"));

        verify(enderChest).clear();
        verify(inv).clear();
    }

    /**
     * Test method for {@link IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testCooldown() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        testExecuteNoConfirmation();
        verify(subCommand).setCooldown("uniqueid", uuid.toString(), 600);
    }
}
