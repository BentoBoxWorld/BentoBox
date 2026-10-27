package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.ChatInput;
import world.bentobox.bentobox.util.TaskScheduler;

class IslandRenamehomeCommandTest extends CommonTestSetup {
    private IslandRenamehomeCommand command;
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = mock(User.class);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.isPlayer()).thenReturn(true);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.isHomeLocation(island, "home")).thenReturn(true);
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);
        command = new IslandRenamehomeCommand(mock(CompositeCommand.class));
    }

    /**
     * Two players asked at the same time must each answer for their own island: the pending
     * input is keyed by player, not stored on the shared command object.
     */
    @Test
    void interleavedRenamePromptsKeepEachUsersIsland() {
        User other = mock(User.class);
        UUID otherId = UUID.randomUUID();
        when(other.getUniqueId()).thenReturn(otherId);
        when(other.isPlayer()).thenReturn(true);
        Island otherIsland = mock(Island.class);
        when(im.getIsland(any(), org.mockito.ArgumentMatchers.eq(other))).thenReturn(otherIsland);
        when(im.isHomeLocation(otherIsland, "home")).thenReturn(true);
        when(otherIsland.getRank(other)).thenReturn(RanksManager.OWNER_RANK);
        when(island.renameHome("home", "mine")).thenReturn(true);
        when(otherIsland.renameHome("home", "theirs")).thenReturn(true);

        assertTrue(command.execute(user, "renamehome", List.of("home")));
        assertTrue(command.execute(other, "renamehome", List.of("home")));
        assertTrue(ChatInput.isWaiting(user.getUniqueId()));
        assertTrue(ChatInput.isWaiting(otherId));

        try (var ignored = Mockito.mockStatic(TaskScheduler.class)) {
            ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
            assertTrue(ChatInput.consume(plugin, user, "mine"));
            assertTrue(ChatInput.consume(plugin, other, "theirs"));
            ignored.verify(() -> TaskScheduler.user(any(), any(), tasks.capture()), Mockito.atLeast(2));
            tasks.getAllValues().forEach(Runnable::run);
        }

        verify(island).renameHome("home", "mine");
        verify(otherIsland).renameHome("home", "theirs");
    }

    @Test
    void executionRechecksRankBeforeOpeningConversation() {
        assertTrue(command.canExecute(user, "renamehome", List.of("home")));
        when(island.getRank(user)).thenReturn(RanksManager.VISITOR_RANK);
        assertFalse(command.execute(user, "renamehome", List.of("home")));
    }
}
