package world.bentobox.bbox.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.Settings;
import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.user.User;
import world.bentobox.bbox.managers.CommandsManager;
import world.bentobox.bbox.managers.IslandWorldManager;
import world.bentobox.bbox.managers.IslandsManager;
import world.bentobox.bbox.managers.LocalesManager;
import world.bentobox.bbox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminClearResetsAllCommandTest {

    private CompositeCommand ac;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;

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
        Settings s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
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
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(),Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
    }

    /**
     * Test method for {@link world.bentobox.bbox.commands.admin.AdminClearResetsAllCommand#execute(world.bentobox.bbox.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteCheckConfirm() {
        AdminClearResetsAllCommand itl = new AdminClearResetsAllCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.confirm"), Mockito.eq("[seconds]"), Mockito.any());
    }

}
