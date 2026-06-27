package world.bentobox.bentobox.api.commands.admin.range;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for {@link AdminRangePurgeBonusCommand}.
 *
 * @author tastybento
 */
class AdminRangePurgeBonusCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandCache islandCache;
    @Mock
    private Island island2;

    private AdminRangePurgeBonusCommand command;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Util.setPlugin(plugin);

        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        Settings s = mock(Settings.class);
        when(s.getConfirmationTime()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        Player p = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        User.setPlugin(plugin);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");

        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);
        when(ac.getTopLabel()).thenReturn("bsb");

        // Two islands in this world carry the "Upgrades" bonus; island2 also has "Other"
        when(island.getWorld()).thenReturn(world);
        when(island2.getWorld()).thenReturn(world);
        when(island.getUniqueId()).thenReturn("island1");
        when(island2.getUniqueId()).thenReturn("island2");
        when(island.getBonusRangeRecord("Upgrades")).thenReturn(Optional.of(new BonusRangeRecord("Upgrades", 50, "")));
        when(island2.getBonusRangeRecord("Upgrades")).thenReturn(Optional.of(new BonusRangeRecord("Upgrades", 50, "")));
        when(island.getBonusRangeRecord("Ghost")).thenReturn(Optional.empty());
        when(island2.getBonusRangeRecord("Ghost")).thenReturn(Optional.empty());
        when(island.getBonusRanges())
                .thenReturn(new ArrayList<>(List.of(new BonusRangeRecord("Upgrades", 50, ""))));
        when(island2.getBonusRanges())
                .thenReturn(new ArrayList<>(List.of(new BonusRangeRecord("Upgrades", 50, ""),
                        new BonusRangeRecord("Other", 10, ""))));

        when(islandCache.getCachedIslands()).thenReturn(List.of(island, island2));
        when(im.getIslandById("island1")).thenReturn(Optional.of(island));
        when(im.getIslandById("island2")).thenReturn(Optional.of(island2));
        when(im.getIslandCache()).thenReturn(islandCache);

        command = new AdminRangePurgeBonusCommand(ac);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCanExecuteNoArgs() {
        assertFalse(command.canExecute(user, "purgebonus", Collections.emptyList()));
    }

    @Test
    void testCanExecuteTooManyArgs() {
        assertFalse(command.canExecute(user, "purgebonus", List.of("Upgrades", "now", "extra")));
    }

    @Test
    void testCanExecuteBadSecondArg() {
        assertFalse(command.canExecute(user, "purgebonus", List.of("Upgrades", "yes")));
    }

    @Test
    void testCanExecuteValidId() {
        assertTrue(command.canExecute(user, "purgebonus", List.of("Upgrades")));
    }

    @Test
    void testCanExecuteValidConfirm() {
        assertTrue(command.canExecute(user, "purgebonus", List.of("Upgrades", "confirm")));
    }

    @Test
    void testCanExecuteRejectedWhileInPurge() {
        command.inPurge = true;
        assertFalse(command.canExecute(user, "purgebonus", List.of("Upgrades")));
        verify(user).sendMessage("commands.admin.range.purgebonus.in-progress");
    }

    @Test
    void testFindIslandIds() {
        List<String> ids = command.findIslandIds(List.of(island, island2), "Upgrades");
        assertEquals(List.of("island1", "island2"), ids);
        assertTrue(command.findIslandIds(List.of(island, island2), "Ghost").isEmpty());
    }

    @Test
    void testConfirmAppliesPurge() {
        // Arrange a pending confirmation as the async scan would have done
        command.toBeConfirmed = true;
        command.pendingId = "Upgrades";
        command.pendingIslandIds = List.of("island1", "island2");

        assertTrue(command.execute(user, "purgebonus", List.of("Upgrades", "confirm")));

        verify(island, times(1)).clearBonusRange("Upgrades");
        verify(island2, times(1)).clearBonusRange("Upgrades");
        verify(user).sendMessage("commands.admin.range.purgebonus.success", "[id]", "Upgrades", "[number]", "2");
        // Pending state cleared
        assertFalse(command.toBeConfirmed);
    }

    @Test
    void testApplyPurgeSkipsIslandsNoLongerCarryingBonus() {
        // island2 no longer has the bonus by the time we apply
        when(island2.getBonusRangeRecord("Upgrades")).thenReturn(Optional.empty());
        command.applyPurge(user, "Upgrades", List.of("island1", "island2"));
        verify(island, times(1)).clearBonusRange("Upgrades");
        verify(island2, never()).clearBonusRange("Upgrades");
        verify(user).sendMessage("commands.admin.range.purgebonus.success", "[id]", "Upgrades", "[number]", "1");
    }

    @Test
    void testTabCompleteListsBonusIds() {
        List<String> ids = command.tabComplete(user, "purgebonus", List.of("")).orElse(Collections.emptyList());
        assertTrue(ids.contains("Upgrades"));
        assertTrue(ids.contains("Other"));
    }

    @Test
    void testTabCompleteFiltersByPrefix() {
        List<String> ids = command.tabComplete(user, "purgebonus", List.of("Up")).orElse(Collections.emptyList());
        assertTrue(ids.contains("Upgrades"));
        assertFalse(ids.contains("Other"));
    }

    @Test
    void testTabCompleteSecondArgSuggestsConfirm() {
        List<String> opts = command.tabComplete(user, "purgebonus", List.of("Upgrades", ""))
                .orElse(Collections.emptyList());
        assertTrue(opts.contains("confirm"));
    }
}
