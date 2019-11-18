package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
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
import world.bentobox.bentobox.Settings;
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
public class AdminDeleteCommandTest {

    private CompositeCommand ac;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private UUID uuid;

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
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

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
        when(ac.getTopLabel()).thenReturn("admin");

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
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(Mockito.any(), Mockito.any(Runnable.class), Mockito.any(Long.class))).thenReturn(task);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
    }


    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testExecuteNoTarget() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        assertFalse(itl.execute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testExecutePlayerNoIsland() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.player-has-no-island"));
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testExecuteOwner() {
        when(im.inTeam(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(notUUID);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.admin.delete.cannot-delete-owner");
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(false);
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(uuid);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);

        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        // First requires confirmation
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
        // Confirm
        itl.execute(user, itl.getLabel(), Arrays.asList(name));
    }

}
