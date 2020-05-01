package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;


/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminRegisterCommandTest {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
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
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
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
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.isOwner(any(),any())).thenReturn(true);
        when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Deletion Manager
        idm = mock(IslandDeletionManager.class);
        when(idm.inDeletion(any())).thenReturn(false);
        when(plugin.getIslandDeletionManager()).thenReturn(idm);

        // Plugin Manager
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq("[name]"), eq("tastybento"));
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecutePlayerHasIsland() {
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.player-has-island"));
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteInTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any())).thenReturn(true);
        when(pm.getUUID(any())).thenReturn(notUUID);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.admin.register.cannot-register-team-player");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteAlreadyOwnedIsland() {
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        Island is = mock(Island.class);
        when(is.getOwner()).thenReturn(uuid);
        when(is.isOwned()).thenReturn(true);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.admin.register.already-owned");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteInDeletionIsland() {
        when(idm.inDeletion(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(pm.getUUID(any())).thenReturn(notUUID);
        Location loc = mock(Location.class);

        // Island has owner
        Island is = mock(Island.class);
        when(is.getOwner()).thenReturn(uuid);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.admin.register.in-deletion");
    }

    /**
     * Test method for {@link AdminRegisterCommand#execute(org.bukkit.command.CommandSender, String, String[])}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        Optional<Island> opi = Optional.of(is);
        when(im.getIslandAt(any())).thenReturn(opi);
        when(user.getLocation()).thenReturn(loc);
        when(pm.getUUID(any())).thenReturn(notUUID);

        AdminRegisterCommand itl = new AdminRegisterCommand(ac);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        // Add other verifications
        verify(user).sendMessage(eq("commands.admin.register.registered-island"), eq("[xyz]"), eq("123,123,432"), eq("[name]"), eq("tastybento"));
        verify(user).sendMessage(eq("general.success"));
    }

}
