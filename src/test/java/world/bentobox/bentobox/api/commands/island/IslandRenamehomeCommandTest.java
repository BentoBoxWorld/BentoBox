package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.admin.conversations.NamePrompt;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

class IslandRenamehomeCommandTest extends CommonTestSetup {
    private IslandRenamehomeCommand command;
    private User user;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        user = mock(User.class);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.isHomeLocation(island, "home")).thenReturn(true);
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);
        command = new IslandRenamehomeCommand(mock(CompositeCommand.class));
    }

    @Test
    void interleavedRenamePromptsKeepEachUsersIsland() {
        User other = mock(User.class);
        Island otherIsland = mock(Island.class);
        when(im.getIsland(any(), org.mockito.ArgumentMatchers.eq(other))).thenReturn(otherIsland);
        when(im.isHomeLocation(otherIsland, "home")).thenReturn(true);
        when(otherIsland.getRank(other)).thenReturn(RanksManager.OWNER_RANK);
        assertTrue(command.canExecute(user, "renamehome", List.of("home")));
        assertTrue(command.canExecute(other, "renamehome", List.of("home")));
        try (var prompts = Mockito.mockConstruction(NamePrompt.class, (prompt, context) -> {
            assertSame(island, context.arguments().get(1));
            assertSame(user, context.arguments().get(2));
        }); var factories = Mockito.mockConstruction(ConversationFactory.class, (factory, context) -> {
            when(factory.withModality(true)).thenReturn(factory);
            when(factory.withLocalEcho(false)).thenReturn(factory);
            when(factory.withTimeout(90)).thenReturn(factory);
            when(factory.withFirstPrompt(any())).thenReturn(factory);
            when(factory.buildConversation(mockPlayer)).thenReturn(mock(Conversation.class));
        })) {
            assertTrue(command.execute(user, "renamehome", List.of("home")));
            org.junit.jupiter.api.Assertions.assertEquals(1, prompts.constructed().size());
        }
    }

    @Test
    void executionRechecksRankBeforeOpeningConversation() {
        assertTrue(command.canExecute(user, "renamehome", List.of("home")));
        when(island.getRank(user)).thenReturn(RanksManager.VISITOR_RANK);
        assertFalse(command.execute(user, "renamehome", List.of("home")));
    }
}
