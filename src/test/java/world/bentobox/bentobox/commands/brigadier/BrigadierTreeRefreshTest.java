package world.bentobox.bentobox.commands.brigadier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Regression tests for sub-commands registered after a top level command's node was built.
 * <p>
 * Addons add their sub-commands to a game mode command while they enable, which happens a tick
 * after Paper's command lifecycle window closed and the tree was handed over. Those sub-commands
 * always ran - the greedy argument catches them - but the client was never told they exist, so
 * they were missing from tab completion. See issue #3039.
 *
 * @author tastybento
 */
class BrigadierTreeRefreshTest {

    private static final String TOP_LABEL = "ai";

    private BentoBox plugin;
    private CommandsManager commandsManager;
    private CompositeCommand topCommand;
    private Map<String, CompositeCommand> subCommands;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private BrigadierCommandRegistrar registrar;

    @BeforeEach
    void setUp() throws Exception {
        // The registrar pushes the tree to online players, which needs a server
        MockBukkit.mock();

        subCommands = new LinkedHashMap<>();
        topCommand = mockCommand(TOP_LABEL, subCommands);
        when(topCommand.getAliases()).thenReturn(List.of("island"));

        commandsManager = mock(CommandsManager.class);
        when(commandsManager.getCommand(TOP_LABEL)).thenReturn(topCommand);
        when(commandsManager.getCommands()).thenReturn(Map.of(TOP_LABEL, topCommand));

        plugin = mock(BentoBox.class);
        when(plugin.getCommandsManager()).thenReturn(commandsManager);

        registrar = new BrigadierCommandRegistrar(plugin);
        dispatcher = new CommandDispatcher<>();
        captureDispatcher(dispatcher);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Stands in for the COMMANDS lifecycle window handing over the live dispatcher.
     */
    private void captureDispatcher(CommandDispatcher<CommandSourceStack> d) throws Exception {
        Field field = BrigadierCommandRegistrar.class.getDeclaredField("dispatcher");
        field.setAccessible(true);
        field.set(registrar, d);
    }

    private CompositeCommand mockCommand(String label, Map<String, CompositeCommand> children) {
        CompositeCommand command = mock(CompositeCommand.class);
        when(command.getLabel()).thenReturn(label);
        when(command.getSubCommands()).thenReturn(children);
        when(command.getPermission()).thenReturn("");
        when(command.isHidden()).thenReturn(false);
        when(command.isOnlyConsole()).thenReturn(false);
        return command;
    }

    private void addSubCommand(String label) {
        subCommands.put(label, mockCommand(label, new LinkedHashMap<>()));
    }

    /**
     * @return the literal child of the top level node, or null if it has none by that name
     */
    private CommandNode<CommandSourceStack> child(String label) {
        CommandNode<CommandSourceStack> top = dispatcher.getRoot().getChild(TOP_LABEL);
        return top == null ? null : top.getChild(label);
    }

    @Test
    void testSubCommandsPresentAtRegistrationAreAdvertised() {
        addSubCommand("go");
        addSubCommand("team");

        registrar.registerCommand(topCommand);

        assertNotNull(child("go"));
        assertNotNull(child("team"));
    }

    /**
     * The state that produced the bug: the addon's sub-command exists on the CompositeCommand but
     * the node was already built without it.
     */
    @Test
    void testSubCommandAddedAfterRegistrationIsNotAdvertisedYet() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);

        addSubCommand("deathchest");

        assertNull(child("deathchest"));
    }

    @Test
    void testRefreshTreesGraftsOnTheLateSubCommand() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);
        addSubCommand("deathchest");

        registrar.refreshTrees();

        assertNotNull(child("deathchest"), "A sub-command added after registration must be advertised");
        assertNotNull(child("go"), "Existing sub-commands must survive the merge");
    }

    /**
     * Brigadier merges same-named literals rather than replacing them, so the greedy catch-all
     * must not end up duplicated by repeated refreshes.
     */
    @Test
    void testRepeatedRefreshesDoNotDuplicateNodes() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);
        int childCount = dispatcher.getRoot().getChild(TOP_LABEL).getChildren().size();

        registrar.refreshTrees();
        registrar.refreshTrees();

        assertEquals(childCount, dispatcher.getRoot().getChild(TOP_LABEL).getChildren().size());
    }

    @Test
    void testRefreshKeepsTheGreedyCatchAll() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);

        registrar.refreshTrees();

        assertNotNull(child("args"), "The catch-all argument must still be there after a refresh");
    }

    @Test
    void testRefreshRegistersACommandThatWasNeverRegistered() {
        addSubCommand("go");
        // Never call registerCommand - the command manager knows about it but the tree does not
        registrar.refreshTrees();

        assertNotNull(dispatcher.getRoot().getChild(TOP_LABEL));
        assertNotNull(child("go"));
    }

    @Test
    void testAliasesStillRedirectAfterARefresh() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);

        registrar.refreshTrees();

        CommandNode<CommandSourceStack> alias = dispatcher.getRoot().getChild("island");
        assertNotNull(alias);
        assertSame(dispatcher.getRoot().getChild(TOP_LABEL), alias.getRedirect(),
                "The alias must still point at the live main node, not a discarded rebuild");
        assertTrue(alias.getChildren().isEmpty());
    }

    @Test
    void testHiddenSubCommandsAreStillNotAdvertised() {
        CompositeCommand hidden = mockCommand("secret", new LinkedHashMap<>());
        when(hidden.isHidden()).thenReturn(true);
        subCommands.put("secret", hidden);

        registrar.registerCommand(topCommand);
        registrar.refreshTrees();

        assertNull(child("secret"));
    }

    /**
     * The merge is a no-op when nothing new turned up, so the tree must come out identical
     * rather than gaining duplicate or replaced nodes.
     */
    @Test
    void testRefreshWithNothingNewLeavesTheTreeAlone() {
        addSubCommand("go");
        registrar.registerCommand(topCommand);
        CommandNode<CommandSourceStack> before = dispatcher.getRoot().getChild(TOP_LABEL);

        registrar.refreshTrees();

        assertSame(before, dispatcher.getRoot().getChild(TOP_LABEL),
                "The live node must be merged into, never swapped out");
        assertEquals(before.getChildren().size(), dispatcher.getRoot().getChild(TOP_LABEL).getChildren().size());
    }

    @Test
    void testRefreshBeforeTheLifecycleWindowIsANoOp() throws Exception {
        captureDispatcher(null);
        addSubCommand("go");

        registrar.refreshTrees();

        assertFalse(registrar.isAvailable());
        assertTrue(dispatcher.getRoot().getChildren().isEmpty());
    }
}
