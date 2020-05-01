package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;


/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminUnregisterCommandTest {

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
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.isOwner(any(),any())).thenReturn(true);
        when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
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

    }

    @After
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        AdminUnregisterCommand itl = new AdminUnregisterCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show help
    }

    /**
     * Test method for {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        AdminUnregisterCommand itl = new AdminUnregisterCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }

    /**
     * Test method for {@link AdminUnregisterCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecutePlayerNoIsland() {
        AdminUnregisterCommand itl = new AdminUnregisterCommand(ac);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage(Mockito.eq("general.errors.player-has-no-island"));
    }

    /**
     * Test method for {@link AdminUnregisterCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccess() {
        when(im.inTeam(any(), any())).thenReturn(false);
        Island is = mock(Island.class);
        Location loc = mock(Location.class);
        when(loc.toVector()).thenReturn(new Vector(123,123,432));
        when(is.getCenter()).thenReturn(loc);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(is);
        String[] name = {"tastybento"};
        when(pm.getUUID(any())).thenReturn(notUUID);

        AdminUnregisterCommand itl = new AdminUnregisterCommand(ac);
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        // Add other verifications
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test method for {@link AdminUnregisterCommand#unregisterPlayer(User, UUID)}.
     */
    @Test
    public void testUnregisterPlayer() {
        @Nullable
        Island oldIsland = mock(Island.class);
        @Nullable
        Location center = mock(Location.class);
        when(oldIsland.getCenter()).thenReturn(center);
        when(center.toVector()).thenReturn(new Vector(1,2,3));
        // Members
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        ImmutableSet<UUID> imSet = ImmutableSet.of(uuid1, uuid2, uuid3);
        when(oldIsland.getMemberSet()).thenReturn(imSet);
        // Trusted member
        UUID uuid4 = UUID.randomUUID();
        // Map must be mutable because it is cleared
        Map<UUID, Integer> map = new HashMap<>();
        map.put(uuid4, RanksManager.TRUSTED_RANK);
        when(oldIsland.getMembers()).thenReturn(map);
        // Island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(oldIsland);
        AdminUnregisterCommand itl = new AdminUnregisterCommand(ac);
        UUID targetUUID = UUID.randomUUID();
        itl.unregisterPlayer(user, "name", targetUUID);
        verify(user).sendMessage("commands.admin.unregister.unregistered-island", "[xyz]", "1,2,3", TextVariables.NAME, "name");
        assertTrue(map.isEmpty());
        verify(im).removePlayer(any(), eq(uuid1));
        verify(im).removePlayer(any(), eq(uuid2));
        verify(im).removePlayer(any(), eq(uuid3));
        verify(pm).clearHomeLocations(any(), eq(uuid1));
        verify(pm).clearHomeLocations(any(), eq(uuid2));
        verify(pm).clearHomeLocations(any(), eq(uuid3));
        verify(im, never()).removePlayer(any(), eq(uuid4));
    }
}
