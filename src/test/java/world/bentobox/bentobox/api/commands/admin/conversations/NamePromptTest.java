package world.bentobox.bentobox.api.commands.admin.conversations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.TaskScheduler;

class NamePromptTest extends RanksManagerTestSetup {
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = mock(User.class);
        when(island.getWorld()).thenReturn(world);
        when(im.getIsland(world, user)).thenReturn(island);
        when(island.getRank(user)).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand("renamehome")).thenReturn(RanksManager.MEMBER_RANK);
    }

    @Test
    void renameWaitsForPlayerOwnerThread() {
        List<Runnable> queued = new ArrayList<>();
        when(island.renameHome("old", "new")).thenReturn(true);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(() -> TaskScheduler.user(any(), any(), any())).thenAnswer(call -> {
                queued.add(call.getArgument(2));
                return null;
            });
            new NamePrompt(plugin, island, user, "old").acceptInput(null, "new");
            verify(island, never()).renameHome(anyString(), anyString());
            queued.getFirst().run();
            verify(island).renameHome("old", "new");
            verify(user).sendMessage("general.success");
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void delayedInputRechecksCurrentIslandAndRequiredRank(boolean changedIsland) {
        List<Runnable> queued = new ArrayList<>();
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            tasks.when(() -> TaskScheduler.user(any(), any(), any())).thenAnswer(call -> {
                queued.add(call.getArgument(2));
                return null;
            });
            new NamePrompt(plugin, island, user, "old", "renamehome").acceptInput(null, "new");
            if (changedIsland) when(im.getIsland(world, user)).thenReturn(null);
            else when(island.getRankCommand("renamehome")).thenReturn(RanksManager.OWNER_RANK);
            queued.getFirst().run();
            verify(island, never()).renameHome(anyString(), anyString());
        }
    }
}
