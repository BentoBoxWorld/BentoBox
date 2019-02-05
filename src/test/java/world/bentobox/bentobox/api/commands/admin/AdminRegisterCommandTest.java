package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
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
import world.bentobox.bentobox.managers.IslandDeletionManager;
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
public class AdminRegisterCommandTest {

    private CompositeCommand ac;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private IslandDeletionManager idm;

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

        // Deletion Manager
        idm = mock(IslandDeletionManager.class);
        when(idm.inDeletion(Mockito.any())).thenReturn(false);
        when(plugin.getIslandDeletionManager()).thenReturn(idm);

        // Plugin Manager
        Server server = mock(Server.class);
        PluginManager pim = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pim);
        when(Bukkit.getServer()).thenReturn(server);

    }


    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteNoTarget() {
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecutePlayerHasIsland() {
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.player-has-island"));
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteInTeam() {
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.register.cannot-register-team-player");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteAlreadyOwnedIsland() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        Island is = mock(Island.class);
        when(is.getOwner()).thenReturn(uuid);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(Mockito.any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.register.already-owned");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteInDeletionIsland() {
        when(idm.inDeletion(Mockito.any())).thenReturn(true);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        Island is = mock(Island.class);
        when(is.getOwner()).thenReturn(uuid);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(Mockito.any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.register.in-deletion");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(Mockito.any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        // Add other verifications
        Mockito.verify(user).sendMessage("commands.admin.register.registered-island", "[xyz]", "123,123,432");
        Mockito.verify(user).sendMessage(Mockito.eq("general.success"));
    }

}
