package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
public class AdminTeamAddCommandTest {

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
        when(ac.getTopLabel()).thenReturn("bsb");

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
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

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
    }


    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteWrongArgs() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        List<String> args = new ArrayList<>();
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
        args.add("arg1");
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
        args.add("args2");
        args.add("args3");
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        // Unknown leader
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(null);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);
        assertFalse(itl.execute(user, ac.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player-name", "[name]", "tastybento");

        // Unknown target
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(null);
        assertFalse(itl.execute(user, ac.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player-name", "[name]", "poslovich");
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteTargetTargetInTeam() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        when(im.inTeam(Mockito.any(), Mockito.eq(notUUID))).thenReturn(true);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.invite.errors.already-on-team"));
    }


    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAddNoIsland() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        // No island,
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.player-has-no-island");

    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAddNotLeader() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        // Has island, has team, but not a leader
        when(im.hasIsland(Mockito.any(),Mockito.eq(uuid))).thenReturn(true);
        when(im.inTeam(Mockito.any(),Mockito.eq(uuid))).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(),Mockito.eq(uuid))).thenReturn(notUUID);

        // Island
        Island island = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(island);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.team.add.name-not-leader", "[name]", "tastybento");
        Mockito.verify(island).showMembers(Mockito.eq(plugin), Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAddTargetHasIsland() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        // Has island, has team, is leader
        when(im.hasIsland(Mockito.any(),Mockito.eq(uuid))).thenReturn(true);
        when(im.inTeam(Mockito.any(),Mockito.eq(uuid))).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.eq(uuid))).thenReturn(uuid);

        // Target has island
        when(im.hasIsland(Mockito.any(), Mockito.eq(notUUID))).thenReturn(true);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.team.add.name-has-island", "[name]", "poslovich");

    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteAddTargetHasIslandNoTeam() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        // Has island, no team
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);

        // Target has island
        when(im.hasIsland(Mockito.any(), Mockito.eq(notUUID))).thenReturn(true);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.team.add.name-has-island", "[name]", "poslovich");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = {"tastybento", "poslovich"};

        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(Mockito.eq("poslovich"))).thenReturn(notUUID);

        // Has island, no team
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);

        // Target has no island
        when(im.hasIsland(Mockito.any(), Mockito.eq(notUUID))).thenReturn(false);

        // Island
        Island island = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(island);

        // Player name
        when(pm.getName(Mockito.eq(uuid))).thenReturn("tastybento");
        when(pm.getName(Mockito.eq(notUUID))).thenReturn("poslovich");
        when(plugin.getPlayers()).thenReturn(pm);

        // Success
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(island).addMember(notUUID);
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }

}
