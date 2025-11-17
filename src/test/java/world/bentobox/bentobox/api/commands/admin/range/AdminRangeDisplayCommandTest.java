package world.bentobox.bentobox.api.commands.admin.range;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.WhiteBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class AdminRangeDisplayCommandTest {

    private CompositeCommand ac;
    private User user;

    /**
     */
    @BeforeEach
    public void setUp() throws Exception {
        ////PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        UUID notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        Answer<String> answer = invocation -> invocation.getArgument(1, String.class);
        when(lm.get(any(), any())).thenAnswer(answer);
        when(plugin.getLocalesManager()).thenReturn(lm);
    }

    @AfterEach
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeDisplayCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerDisplayArgs() {
        AdminRangeDisplayCommand ardc = new AdminRangeDisplayCommand(ac);
        ardc.execute(user, "display", new ArrayList<>());
        // Show display
        verify(user).sendMessage("commands.admin.range.display.showing");
        verify(user).sendMessage("commands.admin.range.display.hint");
        // Run command again
        ardc.execute(user, "display", new ArrayList<>());
        // Remove
        verify(user).sendMessage("commands.admin.range.display.hiding");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeDisplayCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayeShowArgs() {
        AdminRangeDisplayCommand ardc = new AdminRangeDisplayCommand(ac);
        ardc.execute(user, "show", new ArrayList<>());
        // Show display
        verify(user).sendMessage("commands.admin.range.display.showing");
        verify(user).sendMessage("commands.admin.range.display.hint");
        // Run command again
        ardc.execute(user, "show", new ArrayList<>());
        verify(user).sendMessage("commands.admin.range.display.already-on");
        ardc.execute(user, "hide", new ArrayList<>());
        verify(user).sendMessage("commands.admin.range.display.hiding");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.range.AdminRangeDisplayCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecutePlayeHideArgs() {
        AdminRangeDisplayCommand ardc = new AdminRangeDisplayCommand(ac);
        ardc.execute(user, "hide", new ArrayList<>());
        verify(user).sendMessage("commands.admin.range.display.already-off");
    }

}
