package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandTeamPromoteCommandTest extends RanksManagerTestSetup {

    @Mock
    private IslandTeamCommand ic;
    @Mock
    private User user;

    // DUT
    private IslandTeamPromoteCommand ipc;
    private IslandTeamPromoteCommand idc;
    @Mock
    private PlayersManager pm;
    @Mock
    private User target;


    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getWorld()).thenReturn(world);

        // Settings
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getName()).thenReturn("tastybento");
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(pm.getUser("target")).thenReturn(target);
        when(target.getName()).thenReturn("target");
        when(target.getDisplayName()).thenReturn("Target");
        when(target.getUniqueId()).thenReturn(uuid);

        // Manager
        when(plugin.getPlayers()).thenReturn(pm);

        // Translations
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        // Placeholders
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // In team
        when(im.inTeam(world, uuid)).thenReturn(true);
        when(island.inTeam(uuid)).thenReturn(true);

        // IWM
        TestWorldSettings worldSettings = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // Ranks
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.SUB_OWNER_RANK); // Allow sub owners
        when(island.getRank(user)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRank(target)).thenReturn(RanksManager.SUB_OWNER_RANK);

        // Island
        when(im.getIsland(world, user)).thenReturn(island);
        ImmutableSet<UUID> team = ImmutableSet.of(uuid);
        when(island.getMemberSet()).thenReturn(team);

        // Bukkit
        mockedBukkit.when(() -> Bukkit.getOfflinePlayer(uuid)).thenReturn(mockPlayer);
        when(mockPlayer.getName()).thenReturn("tastybento");


        ipc = new IslandTeamPromoteCommand(ic, "promote");
        idc = new IslandTeamPromoteCommand(ic, "demote");

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }  

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#IslandTeamPromoteCommand(world.bentobox.bentobox.api.commands.CompositeCommand, java.lang.String)}.
     */
    @Test
    public void testIslandTeamPromoteCommand() {
        assertNotNull(ipc);
        assertNotNull(idc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("island.team.promote", ipc.getPermission());
        assertEquals("island.team.promote", idc.getPermission());
        assertTrue(ipc.isOnlyPlayer());
        assertTrue(idc.isOnlyPlayer());
        assertEquals("commands.island.team.promote.parameters", ipc.getParameters());
        assertEquals("commands.island.team.demote.parameters", idc.getParameters());
        assertEquals("commands.island.team.promote.description", ipc.getDescription());
        assertEquals("commands.island.team.demote.description", idc.getDescription());
        assertTrue(ipc.isConfigurableRankCommand());
        assertTrue(idc.isConfigurableRankCommand());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringShowHelp() {
        assertFalse(ipc.canExecute(user, "promote", List.of())); // Nothing
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringNoTeam() {
        when(im.inTeam(any(), any())).thenReturn(false);
        assertFalse(ipc.canExecute(user, "promote", List.of("tastybento")));
        verify(user).sendMessage("general.errors.no-team");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringInsufficientRank() {
        when(island.getRank(user)).thenReturn(RanksManager.MEMBER_RANK);
        assertFalse(ipc.canExecute(user, "promote", List.of("tastybento")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringUnknownPlayer() {
        when(pm.getUser(anyString())).thenReturn(null);
        assertFalse(ipc.canExecute(user, "promote", List.of("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringSameUser() {
        when(pm.getUser(anyString())).thenReturn(user);
        assertFalse(ipc.canExecute(user, "promote", List.of("tastybento")));
        verify(user).sendMessage("commands.island.team.promote.errors.cant-promote-yourself");
        assertFalse(idc.canExecute(user, "demote", List.of("tastybento")));
        verify(user).sendMessage("commands.island.team.demote.errors.cant-demote-yourself");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringDemoteOwner() {
        when(island.getRank(target)).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(idc.canExecute(user, "demote", List.of("target")));
        verify(user).sendMessage("commands.island.team.demote.errors.cant-demote");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringPromoteAboveSelf() {
        when(island.getRank(target)).thenReturn(RanksManager.SUB_OWNER_RANK);
        assertFalse(ipc.canExecute(user, "promote", List.of("target")));
        verify(user).sendMessage("commands.island.team.promote.errors.cant-promote");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringSuccess() {
        when(island.getRank(target)).thenReturn(RanksManager.MEMBER_RANK);
        assertTrue(ipc.canExecute(user, "promote", List.of("target")));
        assertTrue(idc.canExecute(user, "demote", List.of("target")));
        verify(user, never()).sendMessage(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        when(island.getRank(target)).thenReturn(RanksManager.MEMBER_RANK);
        when(rm.getRankUpValue(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.SUB_OWNER_RANK);
        ipc.canExecute(user, "promote", List.of("target"));
        assertTrue(ipc.execute(user, "promote", List.of("target")));
        verify(island).setRank(target, RanksManager.SUB_OWNER_RANK);
        verify(user).sendMessage("commands.island.team.promote.success", TextVariables.NAME, "target",
                TextVariables.RANK, "", TextVariables.DISPLAY_NAME, "Target");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfStringNoIsland() {
        when(im.getIsland(world, user)).thenReturn(null);
        Optional<List<String>> options = ipc.tabComplete(user, "promote", List.of("p"));
        assertTrue(options.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamPromoteCommand#tabComplete(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testTabCompleteUserStringListOfString() {
        Optional<List<String>> options = ipc.tabComplete(user, "promote", List.of("p"));
        assertFalse(options.isEmpty());
        assertTrue(options.get().isEmpty());

        options = ipc.tabComplete(user, "promote", List.of("t"));
        assertFalse(options.isEmpty());
        assertFalse(options.get().isEmpty());
        assertEquals("tastybento", options.get().getFirst());
    }

}
