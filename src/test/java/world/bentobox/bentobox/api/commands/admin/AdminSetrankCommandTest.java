package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class AdminSetrankCommandTest {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;

    private RanksManager rm;
    private AdminSetrankCommand c;

    private UUID targetUUID;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        // Ranks Manager
        rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

        // Players Manager
        when(plugin.getPlayers()).thenReturn(pm);

        // Islands manager
        when(plugin.getIslands()).thenReturn(im);

        // Target
        targetUUID = UUID.randomUUID();
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(targetUUID);
        User.getInstance(p);

        // Online players
        PowerMockito.mockStatic(Util.class);
        when(Util.getOnlinePlayerList(any())).thenReturn(Collections.singletonList("tastybento"));
        when(Util.getUUID(anyString())).thenCallRealMethod();

        // Translations
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Command
        c = new AdminSetrankCommand(ac);

        // Plugin Manager
        PowerMockito.mockStatic(Bukkit.class);
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#AdminSetrankCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminSetrankCommand() {
        assertEquals("setrank", c.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("admin.setrank", c.getPermission());
        assertFalse(c.isOnlyPlayer());
        assertEquals("commands.admin.setrank.parameters", c.getParameters());
        assertEquals("commands.admin.setrank.description", c.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoArgs() {
        assertFalse(c.canExecute(user, "", Collections.emptyList()));
        verify(user).getTranslation("commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOneArg() {
        assertFalse(c.canExecute(user, "", Collections.singletonList("test")));
        verify(user).getTranslation("commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        assertFalse(c.canExecute(user, "", Arrays.asList("tastybento", "member")));
        verify(user).sendMessage("general.errors.unknown-player",
                "[name]",
                "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerNoIsland() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        assertFalse(c.canExecute(user, "", Arrays.asList("tastybento", "ranks.member")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerHasIslandUnknownRank() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        assertFalse(c.canExecute(user, "", Arrays.asList("tastybento", "xxx")));
        verify(user).sendMessage("commands.admin.setrank.unknown-rank");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerHasIslandTooLowRank() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        assertFalse(c.canExecute(user, "", Arrays.asList("tastybento", "ranks.visitor")));
        verify(user).sendMessage("commands.admin.setrank.not-possible");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerHasIslandSuccess() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        assertTrue(c.canExecute(user, "", Arrays.asList("tastybento", "ranks.member")));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        // Set the target
        testCanExecuteKnownPlayerHasIslandSuccess();
        Island island = mock(Island.class);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        assertTrue(c.execute(user, "", Arrays.asList("tastybento", "member")));
        verify(user).sendMessage(eq("commands.admin.setrank.rank-set"),
                eq("[from]"),
                eq("ranks.sub-owner"),
                eq("[to]"),
                eq("ranks.member"),
                eq("[name]"),
                eq(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminSetrankCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        Optional<List<String>> result = c.tabComplete(user, "", Arrays.asList("setrank", ""));
        assertTrue(result.isPresent());
        result.ifPresent(list -> {
            assertTrue(list.size() == 1);
            assertEquals("tastybento", list.get(0));
        });
    }

}
