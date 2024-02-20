package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
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
@PrepareForTest({ Bukkit.class, BentoBox.class })
public class AdminUnregisterCommandTest {

    private UUID uuid = UUID.randomUUID();
    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private World world;
    @Mock
    private Island island;
    @Mock
    private Island island2;
    @Mock
    private @NonNull Location location1;
    @Mock
    private @NonNull Location location2;
    private AdminUnregisterCommand itl;

    /**
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

        // Settings
        Settings s = mock(Settings.class);
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        when(user.isOp()).thenReturn(false);

        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        User.setPlugin(plugin);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(pm.getUUID("tastybento")).thenReturn(uuid);
        User.getInstance(p);


        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // when(im.isOwner(any(),any())).thenReturn(true);
        // when(im.getOwner(any(),any())).thenReturn(uuid);
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

        // Plugin Manager
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Islands
        when(location1.toVector()).thenReturn(new Vector(1, 2, 3));
        when(location2.toVector()).thenReturn(new Vector(4, 5, 6));
        when(island.getCenter()).thenReturn(location1);
        when(island2.getCenter()).thenReturn(location2);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid, notUUID));
        when(island2.getMemberSet()).thenReturn(ImmutableSet.of(uuid, notUUID));
        when(im.getOwnedIslands(world, uuid)).thenReturn(Set.of(island, island2));

        // Player Manager
        when(pm.getName(uuid)).thenReturn("name");
        when(pm.getName(notUUID)).thenReturn("name2");

        itl = new AdminUnregisterCommand(ac);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show help
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        String[] name = { "tastybento" };
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecutePlayerFailNoIsland() {
        // No island
        when(im.getOwnedIslands(world, uuid)).thenReturn(Set.of());
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecutePlayerFailMoreIsland() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
        verify(user).sendMessage("commands.admin.unregister.errors.specify-island-location");
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecutePlayerFailWrongIsland() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "1,2,4")));
        verify(user).sendMessage("commands.admin.unregister.errors.unknown-island-location");
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteDiffernetPlayerFailWrongIsland() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("BoxManager", "1,2,4")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "BoxManager");
    }

    /**
     * Test method for
     * {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerSuccessMultiIsland() {
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "1,2,3")));
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "4,5,6")));
    }

    /**
     * Test method for {@link AdminUnregisterCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessOneIsland() {
        when(im.getOwnedIslands(world, uuid)).thenReturn(Set.of(island));
        itl.canExecute(user, itl.getLabel(), List.of("tastybento"));
        assertTrue(itl.execute(user, itl.getLabel(), List.of("tastybento")));
        // Add other verifications
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test method for {@link AdminUnregisterCommand#unregisterIsland(User)}
     */
    @Test
    public void testUnregisterIsland() {
        this.testExecuteSuccessOneIsland();
        itl.unregisterIsland(user);
        verify(user).sendMessage("commands.admin.unregister.unregistered-island", TextVariables.XYZ, "1,2,3",
                TextVariables.NAME, "name");
        verify(island).setOwner(null);
    }

    /**
     * Test method for {@link AdminUnregisterCommand#unregisterIsland(User)}
     */
    @Test
    public void testUnregisterIslandMulti() {
        this.testExecutePlayerSuccessMultiIsland();
        itl.unregisterIsland(user);
        verify(user).sendMessage("commands.admin.unregister.unregistered-island", TextVariables.XYZ, "4,5,6",
                TextVariables.NAME, "name");
    }
}
