package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminTeamSetMaxSizeCommand}.
 */
class AdminTeamSetMaxSizeCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private UUID targetUUID;
    private AdminTeamSetMaxSizeCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player / user
        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bsb");

        // Players manager
        targetUUID = UUID.randomUUID();
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(targetUUID)).thenReturn("target");

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // IWM
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(iwm.getMaxTeamSize(any())).thenReturn(4);
        @NonNull
        WorldSettings ws = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Island
        when(im.getPrimaryIsland(any(), eq(targetUUID))).thenReturn(island);

        cmd = new AdminTeamSetMaxSizeCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testExecuteWrongArgCount() {
        // No args
        assertFalse(cmd.execute(user, cmd.getLabel(), new ArrayList<>()));
        // Only one arg
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("target")));
        // Three args
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("target", "5", "extra")));
    }

    @Test
    void testExecuteUnknownPlayer() {
        when(pm.getUUID("unknown")).thenReturn(null);
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("unknown", "5")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "unknown");
    }

    @Test
    void testExecuteInvalidNumber() {
        when(pm.getUUID("target")).thenReturn(targetUUID);
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("target", "notanumber")));
        verify(user).sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, "notanumber");
    }

    @Test
    void testExecuteNegativeNumber() {
        when(pm.getUUID("target")).thenReturn(targetUUID);
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("target", "-1")));
        verify(user).sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, "-1");
    }

    @Test
    void testExecutePlayerHasNoIsland() {
        when(pm.getUUID("target")).thenReturn(targetUUID);
        when(im.getPrimaryIsland(any(), eq(targetUUID))).thenReturn(null);
        assertFalse(cmd.execute(user, cmd.getLabel(), List.of("target", "5")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testExecuteSetSuccess() {
        when(pm.getUUID("target")).thenReturn(targetUUID);

        assertTrue(cmd.execute(user, cmd.getLabel(), List.of("target", "5")));
        verify(im).setMaxMembers(island, RanksManager.MEMBER_RANK, 5);
        verify(user).sendMessage("commands.admin.team.maxsize.success",
                TextVariables.NAME, "target",
                TextVariables.NUMBER, "5");
    }

    @Test
    void testExecuteResetToWorldDefault() {
        when(pm.getUUID("target")).thenReturn(targetUUID);

        // Using 0 should reset to world default
        assertTrue(cmd.execute(user, cmd.getLabel(), List.of("target", "0")));
        verify(im).setMaxMembers(island, RanksManager.MEMBER_RANK, null);
        verify(user).sendMessage("commands.admin.team.maxsize.reset",
                TextVariables.NAME, "target",
                TextVariables.NUMBER, "4");
        verify(user, never()).sendMessage(eq("commands.admin.team.maxsize.success"),
                any(), any(), any(), any());
    }
}
