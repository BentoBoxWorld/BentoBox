package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
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
public class AdminTeamSetownerCommandTest {

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
        BentoBox plugin = mock(BentoBox.class);
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
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(),Mockito.any())).thenReturn(uuid);
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

        // Plugin Manager
        Server server = mock(Server.class);
        PluginManager pim = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pim);
        when(Bukkit.getServer()).thenReturn(server);

    }


    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        AdminTeamSetownerCommand itl = new AdminTeamSetownerCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminTeamSetownerCommand itl = new AdminTeamSetownerCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecutePlayerNotInTeam() {
        AdminTeamSetownerCommand itl = new AdminTeamSetownerCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(new HashSet<>());
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.not-in-team"));
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteMakeOwnerAlreadyOwner() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        when(im.getOwner(Mockito.any(), Mockito.eq(notUUID))).thenReturn(notUUID);

        AdminTeamSetownerCommand itl = new AdminTeamSetownerCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.team.setowner.already-owner");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecuteSuccess() {
        // Player is a team member, not an owner
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        Island is = mock(Island.class);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        // Owner
        when(im.getOwner(Mockito.any(), Mockito.eq(notUUID))).thenReturn(uuid);
        when(pm.getName(Mockito.eq(uuid))).thenReturn("owner");
        // Members
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(notUUID);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);

        AdminTeamSetownerCommand itl = new AdminTeamSetownerCommand(ac);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        // Add other verifications
        Mockito.verify(im).setOwner(Mockito.any(), Mockito.eq(user), Mockito.eq(notUUID));
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }

}
