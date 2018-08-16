package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminInfoCommandTest {

    private BentoBox plugin;
    private CompositeCommand ac;
    private UUID uuid;
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
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
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
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
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
     * Test method for {@link AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTargetConsole() {
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        CommandSender sender = mock(CommandSender.class);
        User console = User.getInstance(sender);
        assertFalse(itl.execute(console, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminInfoCommand#execute(User, String, List)} .
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.unknown-player"));
    }

    /**
     * Test method for .
     */
    @Test
    public void testExecutePlayerHasNoIsland() {
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.player-has-no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(notUUID))).thenReturn(is);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(is).showInfo(Mockito.eq(plugin), Mockito.eq(user), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteUserNotOnIsland() {
        when(user.isPlayer()).thenReturn(true);
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        // No island here
        when(im.getIslandAt(Mockito.any())).thenReturn(Optional.empty());
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Confirm other verifications
        Mockito.verify(user).sendMessage("commands.admin.info.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminInfoCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccessUserOnIsland() {
        when(user.isPlayer()).thenReturn(true);
        AdminInfoCommand itl = new AdminInfoCommand(ac);
        Location loc = mock(Location.class);

        // Island has owner
        Island is = mock(Island.class);
        when(is.getOwner()).thenReturn(uuid);
        when(is.showInfo(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(Mockito.any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);


        assertTrue(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Confirm other verifications
        Mockito.verify(is).showInfo(Mockito.eq(plugin), Mockito.eq(user), Mockito.any());
    }
}