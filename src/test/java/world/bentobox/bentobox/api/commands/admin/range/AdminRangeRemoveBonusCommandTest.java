package world.bentobox.bentobox.api.commands.admin.range;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.BonusRangeRecord;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminRangeRemoveBonusCommand}.
 *
 * @author tastybento
 */
class AdminRangeRemoveBonusCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private AdminRangeRemoveBonusCommand command;
    private List<BonusRangeRecord> bonusRanges;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings - confirmation time of 0 seconds
        Settings s = mock(Settings.class);
        when(s.getConfirmationTime()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        User.setPlugin(plugin);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");

        // Players manager
        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(plugin.getPlayers()).thenReturn(pm);

        // Parent command has no aliases and a known top label (needed for confirmation matching)
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);
        when(ac.getTopLabel()).thenReturn("bsb");

        // Island has a couple of bonus ranges to begin with
        bonusRanges = new ArrayList<>();
        BonusRangeRecord level = new BonusRangeRecord("Level", 50, "Reward");
        bonusRanges.add(level);
        bonusRanges.add(new BonusRangeRecord("Upgrades", 50, "Upgrade"));
        when(island.getBonusRanges()).thenReturn(bonusRanges);
        when(island.getBonusRangeRecord("Level")).thenReturn(Optional.of(level));
        when(island.getBonusRangeRecord("Upgrades")).thenReturn(Optional.of(bonusRanges.get(1)));
        when(island.getBonusRange("Level")).thenReturn(50);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        command = new AdminRangeRemoveBonusCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCanExecuteNoArgs() {
        assertFalse(command.canExecute(user, "removebonus", Collections.emptyList()));
    }

    @Test
    void testCanExecuteTooManyArgs() {
        assertFalse(command.canExecute(user, "removebonus", List.of("tastybento", "Level", "extra")));
    }

    @Test
    void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(command.canExecute(user, "removebonus", List.of("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    @Test
    void testCanExecuteNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(command.canExecute(user, "removebonus", List.of("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testCanExecuteNoBonus() {
        bonusRanges.clear();
        assertFalse(command.canExecute(user, "removebonus", List.of("tastybento")));
        verify(user).sendMessage("commands.admin.range.removebonus.no-bonus");
    }

    @Test
    void testCanExecuteUnknownBonusId() {
        when(island.getBonusRangeRecord("Nope")).thenReturn(Optional.empty());
        assertFalse(command.canExecute(user, "removebonus", List.of("tastybento", "Nope")));
        verify(user).sendMessage("commands.admin.range.removebonus.unknown-bonus", "[id]", "Nope");
    }

    @Test
    void testCanExecuteSuccessAll() {
        assertTrue(command.canExecute(user, "removebonus", List.of("tastybento")));
    }

    @Test
    void testCanExecuteSuccessId() {
        assertTrue(command.canExecute(user, "removebonus", List.of("tastybento", "Level")));
    }

    @Test
    void testExecuteAsksConfirmationDoesNotRemoveYet() {
        assertTrue(command.canExecute(user, "removebonus", List.of("tastybento")));
        assertTrue(command.execute(user, "removebonus", List.of("tastybento")));
        // Nothing removed until confirmed
        verify(island, never()).clearAllBonusRanges();
        verify(island, never()).clearBonusRange(any());
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    @Test
    void testRemoveAllBonusRanges() {
        assertTrue(command.canExecute(user, "removebonus", List.of("tastybento")));
        command.removeBonusRanges(user, "tastybento");
        verify(island, times(1)).clearAllBonusRanges();
        verify(island, never()).clearBonusRange(any());
        verify(user).sendMessage("commands.admin.range.removebonus.success", "[number]", "100", "[name]",
                "tastybento");
    }

    @Test
    void testRemoveBonusRangeById() {
        assertTrue(command.canExecute(user, "removebonus", List.of("tastybento", "Level")));
        command.removeBonusRanges(user, "tastybento");
        verify(island, times(1)).clearBonusRange("Level");
        verify(island, never()).clearAllBonusRanges();
        verify(user).sendMessage("commands.admin.range.removebonus.success-id", "[id]", "Level", "[number]", "50",
                "[name]", "tastybento");
    }

    @Test
    void testTabCompleteEmptyArgs() {
        assertTrue(command.tabComplete(user, "removebonus", Collections.emptyList()).isEmpty());
    }

    @Test
    void testTabCompleteBonusIds() {
        List<String> ids = command.tabComplete(user, "removebonus", List.of("tastybento", "L"))
                .orElse(Collections.emptyList());
        assertTrue(ids.contains("Level"));
    }
}
