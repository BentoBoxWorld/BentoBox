package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
public class IslandTeamLeaveCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private CompositeCommand subCommand;
    @Mock
    private PlayersManager pm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

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
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        Optional<CompositeCommand> optionalCommand = Optional.of(subCommand);
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(optionalCommand);
        when(ic.getWorld()).thenReturn(world);

        // Player has island to begin with
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        
        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Island World Manager
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Island
        when(island.getUniqueId()).thenReturn("uniqueid");
        when(im.getIsland(world, user)).thenReturn(island);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamLeaveCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteIsOwner() {
        when(island.getOwner()).thenReturn(uuid);
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

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(im).removePlayer(island,uuid);
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

        IslandTeamLeaveCommand itl = new IslandTeamLeaveCommand(ic);
        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        verify(im).removePlayer(island, uuid);
        verify(user).sendMessage("commands.island.team.leave.success");
        verify(pm).addReset(eq(world), eq(uuid));
        verify(user).sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, "100");
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
