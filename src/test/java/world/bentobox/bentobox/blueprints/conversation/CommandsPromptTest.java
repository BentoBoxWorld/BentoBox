package world.bentobox.bentobox.blueprints.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Tests for {@link CommandsPrompt}.
 */
class CommandsPromptTest extends CommonTestSetup {

    @Mock
    private GameModeAddon addon;
    @Mock
    private BlueprintBundle bb;
    @Mock
    private ConversationContext context;

    private CommandsPrompt prompt;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Use mockPlayer from CommonTestSetup - it is already registered as a User
        when(context.getForWhom()).thenReturn(mockPlayer);
        when(bb.getDisplayName()).thenReturn("Test Bundle");
        prompt = new CommandsPrompt(addon, bb);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetPromptTextFirstCall() {
        when(context.getSessionData("commands")).thenReturn(null);
        String text = prompt.getPromptText(context);
        // Locale manager returns the key, so we get the translation key
        assertInstanceOf(String.class, text);
    }

    @Test
    void testGetPromptTextWithSessionData() {
        when(context.getSessionData("commands")).thenReturn(List.of("say hello"));
        String text = prompt.getPromptText(context);
        assertInstanceOf(String.class, text);
        // Should contain the command text in the output
        assertEquals(true, text.contains("say hello"));
    }

    @Test
    void testAcceptInputQuit() {
        // The translation of quit key is the key itself (locale mock returns key)
        when(context.getSessionData("commands")).thenReturn(null);
        Prompt next = prompt.acceptInput(context,
                "commands.admin.blueprint.management.commands.quit");
        assertInstanceOf(CommandsSuccessPrompt.class, next);
    }

    @Test
    void testAcceptInputClear() {
        List<String> existing = new ArrayList<>(List.of("old command"));
        when(context.getSessionData("commands")).thenReturn(existing);
        Prompt next = prompt.acceptInput(context,
                "commands.admin.blueprint.management.commands.clear");
        // Should return this (same prompt) after clearing
        assertInstanceOf(CommandsPrompt.class, next);
        verify(context).setSessionData("commands", new ArrayList<>());
    }

    @Test
    void testAcceptInputAddsCommand() {
        when(context.getSessionData("commands")).thenReturn(null);
        Prompt next = prompt.acceptInput(context, "say hello");
        assertInstanceOf(CommandsPrompt.class, next);
        verify(context).setSessionData(eq("commands"), any());
    }

    @Test
    void testAcceptInputAppendsToExistingList() {
        List<String> existing = new ArrayList<>(List.of("first command"));
        when(context.getSessionData("commands")).thenReturn(existing);
        Prompt next = prompt.acceptInput(context, "second command");
        assertInstanceOf(CommandsPrompt.class, next);
        assertEquals(2, existing.size());
        assertEquals("second command", existing.get(1));
    }
}
