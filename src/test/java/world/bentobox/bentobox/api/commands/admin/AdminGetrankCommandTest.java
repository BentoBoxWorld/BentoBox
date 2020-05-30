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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class AdminGetrankCommandTest {

    private static final String[] NAMES = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;

    @Mock
    private RanksManager rm;
    private AdminGetrankCommand c;

    private UUID targetUUID;

    @Mock
    private Island island;

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

        // Bukkit - online players
        Map<UUID, String> online = new HashMap<>();

        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < NAMES.length; j++) {
            Player p1 = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p1.getUniqueId()).thenReturn(uuid);
            when(p1.getName()).thenReturn(NAMES[j]);
            online.put(uuid, NAMES[j]);
            onlinePlayers.add(p1);
        }
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

        // Command
        c = new AdminGetrankCommand(ac);
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
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#AdminGetrankCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminGetrankCommand() {
        assertEquals("getrank", c.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("admin.getrank", c.getPermission());
        assertFalse(c.isOnlyPlayer());
        assertEquals("commands.admin.getrank.parameters", c.getParameters());
        assertEquals("commands.admin.getrank.description", c.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoArgs() {
        assertFalse(c.canExecute(user, "", Collections.emptyList()));
        verify(user).getTranslation("commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        assertFalse(c.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player",
                "[name]",
                "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerNoIsland() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        assertFalse(c.canExecute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnownPlayerHasIslandSuccess() {
        when(pm.getUUID(any())).thenReturn(targetUUID);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(user.getTranslation(anyString())).thenReturn("member");
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        assertTrue(c.canExecute(user, "", Collections.singletonList("tastybento")));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        // Set the target
        testCanExecuteKnownPlayerHasIslandSuccess();
        when(island.getRank(any(User.class))).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(user.getTranslation(any())).thenReturn("sub-owner", "sub-owner");
        when(island.getOwner()).thenReturn(targetUUID);
        when(pm.getName(targetUUID)).thenReturn("tastybento");
        assertTrue(c.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.admin.getrank.rank-is"),
                eq("[rank]"),
                eq("sub-owner"),
                eq("[name]"),
                eq("tastybento"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringNoChars() {
        Optional<List<String>> result = c.tabComplete(user, "", Collections.emptyList());
        assertFalse(result.isPresent());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.AdminGetrankCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringWithChars() {
        Optional<List<String>> result = c.tabComplete(user, "", Collections.singletonList("g"));
        assertTrue(result.isPresent());
        result.ifPresent(list -> {
            assertEquals(1, list.size());
        });
        // Two names
        result = c.tabComplete(user, "", Collections.singletonList("f"));
        assertTrue(result.isPresent());
        result.ifPresent(list -> {
            assertEquals(2, list.size());
        });
    }

}
