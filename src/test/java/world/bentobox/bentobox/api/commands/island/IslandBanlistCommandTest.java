package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandBanlistCommandTest extends RanksManagerTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(-1); // Unlimited bans
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link IslandBanlistCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testWithArgs() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        // Verify show help
        verify(user).sendMessage("commands.help.header", "[label]", "commands.help.console");
    }

    /**
     * Test method for {@link IslandBanlistCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testNoIsland() {
        // not in team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link IslandBanlistCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testTooLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for
     * {@link IslandBanlistCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testBanlistNooneBanned() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        iubc.canExecute(user, iubc.getLabel(), Collections.emptyList());
        assertTrue(iubc.execute(user, iubc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.banlist.noone");
    }

    /**
     * Test method for
     * {@link IslandBanlistCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testBanlistBanned() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Make a ban list
        String[] names = { "adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe" };
        Set<UUID> banned = new HashSet<>();
        Map<UUID, String> uuidToName = new HashMap<>();
        for (String name : names) {
            UUID uuid = UUID.randomUUID();
            banned.add(uuid);
            uuidToName.put(uuid, name);
        }
        when(island.getBanned()).thenReturn(banned);
        // Respond to name queries
        when(pm.getName(any(UUID.class))).then((Answer<String>) invocation -> uuidToName
                .getOrDefault(invocation.getArgument(0, UUID.class), "tastybento"));
        iubc.canExecute(user, iubc.getLabel(), Collections.emptyList());
        assertTrue(iubc.execute(user, iubc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.banlist.the-following");
    }

    /**
     * Test method for
     * {@link IslandBanlistCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testBanlistMaxBanNoLimit() {
        testBanlistBanned();
        verify(user, never()).sendMessage(eq("commands.island.banlist.you-can-ban"), anyString(), anyString());
    }

    /**
     * Test method for {@link IslandBanlistCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testBanlistMaxBanLimit() {
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(15);
        testBanlistBanned();
        verify(user).sendMessage(eq("commands.island.banlist.you-can-ban"), eq(TextVariables.NUMBER), eq("4"));
    }
}
