package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class })
public class AdminDeleteCommandTest {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    private UUID uuid;
    @Mock
    private World world;
    @Mock
    private @Nullable Island island;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Util
        Util.setPlugin(plugin);

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
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("admin");
        when(ac.getWorld()).thenReturn(world);

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // when(im.isOwner(any(),any())).thenReturn(true);
        // when(im.getOwner(any(),any())).thenReturn(uuid);
        when(im.getIsland(world, user)).thenReturn(island);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island));
        when(plugin.getIslands()).thenReturn(im);

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.hasTeam()).thenReturn(true);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testExecuteNoTarget() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show help
    }

    /**
     * Test method for
     * {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        String[] name = { "tastybento" };
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for
     * {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testExecutePlayerNoIsland() {
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.hasIsland(world, notUUID)).thenReturn(false);
        when(im.inTeam(world, notUUID)).thenReturn(false);
        assertFalse(itl.canExecute(user, "", List.of("tastybento")));
        verify(user).sendMessage(eq("general.errors.player-has-no-island"));
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testExecuteOwner() {
        when(im.hasIsland(world, notUUID)).thenReturn(true);
        when(im.inTeam(world, notUUID)).thenReturn(true);
        when(island.getOwner()).thenReturn(notUUID);
        when(island.hasTeam()).thenReturn(true);
        when(pm.getUUID(any())).thenReturn(notUUID);
        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.delete.cannot-delete-owner");
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testcanExecuteSuccessUUID() {
        when(im.hasIsland(world, uuid)).thenReturn(true);
        when(island.hasTeam()).thenReturn(false);
        when(im.inTeam(any(), any())).thenReturn(false);
        //when(im.getOwner(any(), any())).thenReturn(uuid);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIslands(any(), any(UUID.class))).thenReturn(List.of(is));
        // No such name
        when(pm.getUUID(any())).thenReturn(null);

        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        // Success because it's a valid UUID
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList(uuid.toString())));
    }

    /**
     * Test method for {@link AdminDeleteCommand#canExecute(User, String, java.util.List)
     */
    @Test
    public void testExecuteFailUUID() {
        when(im.inTeam(any(), any())).thenReturn(false);
        //when(im.getOwner(any(), any())).thenReturn(uuid);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        // No such name
        when(pm.getUUID(any())).thenReturn(null);

        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        // Fail because it's not a UUID
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("not-A-UUID")));
    }

    /**
     * Test method for {@link AdminDeleteCommand#execute(User, String, java.util.List)
     */
    @Test
    public void testCanExecuteSuccess() {
        when(island.hasTeam()).thenReturn(false);
        when(im.inTeam(any(), any())).thenReturn(false);
        //when(im.getOwner(any(), any())).thenReturn(uuid);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);

        AdminDeleteCommand itl = new AdminDeleteCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        // Confirm
        itl.execute(user, itl.getLabel(), Arrays.asList(name));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

}
