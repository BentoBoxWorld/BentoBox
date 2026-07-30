package world.bentobox.bentobox.commands.brigadier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import io.papermc.paper.command.brigadier.Commands;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Regression tests for BentoBox commands vanishing from the Bukkit command map.
 * <p>
 * Every command is registered with the command map first, and on Paper that is a
 * view over this dispatcher, so by the time the tree is built Paper already holds
 * a node under the label. That node has to survive: it is what carries the
 * {@link CompositeCommand} back to anything dispatching through the command map -
 * NPC plugins, command signs, GUI plugins - and a plain literal in its place
 * makes Paper synthesise a {@code VanillaCommandWrapper} that demands
 * {@code minecraft.command.<label>} and refuses the command with the server's own
 * no-permission message. See issue #3050.
 *
 * @author tastybento
 */
class BrigadierCommandMapMergeTest {

    private static final String TOP_LABEL = "ai";
    private static final String ALIAS = "island";

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
        when(topCommand.getAliases()).thenReturn(List.of(ALIAS));

        CommandsManager commandsManager = mock(CommandsManager.class);
        when(commandsManager.getCommand(TOP_LABEL)).thenReturn(topCommand);
        when(commandsManager.getCommands()).thenReturn(Map.of(TOP_LABEL, topCommand));

        BentoBox plugin = mock(BentoBox.class);
        when(plugin.getCommandsManager()).thenReturn(commandsManager);

        registrar = new BrigadierCommandRegistrar(plugin);
        injectDispatcher(new CommandDispatcher<>());
    }

    /**
     * Stands in for the COMMANDS lifecycle window handing over the live dispatcher.
     */
    private void injectDispatcher(CommandDispatcher<CommandSourceStack> live) throws Exception {
        Field field = BrigadierCommandRegistrar.class.getDeclaredField("dispatcher");
        field.setAccessible(true);
        field.set(registrar, live);
        this.dispatcher = live;
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
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
     * Stands in for the node Paper puts in the dispatcher when the command is
     * registered with the command map.
     */
    private CommandNode<CommandSourceStack> givenCommandMapNode(String label) {
        return dispatcher.register(Commands.literal(label).executes(ctx -> 1));
    }

    @Test
    void testTheCommandMapNodeIsMergedIntoNotReplaced() {
        CommandNode<CommandSourceStack> existing = givenCommandMapNode(TOP_LABEL);
        addSubCommand("go");

        registrar.registerCommand(topCommand);

        assertSame(existing, dispatcher.getRoot().getChild(TOP_LABEL),
                "Paper's node carries the command back to the command map and must not be swapped out");
    }

    @Test
    void testSubCommandsAreGraftedOntoTheCommandMapNode() {
        givenCommandMapNode(TOP_LABEL);
        addSubCommand("go");
        addSubCommand("team");

        registrar.registerCommand(topCommand);

        CommandNode<CommandSourceStack> top = dispatcher.getRoot().getChild(TOP_LABEL);
        assertNotNull(top.getChild("go"));
        assertNotNull(top.getChild("team"));
        assertNotNull(top.getChild("args"), "The catch-all must be grafted on too");
    }

    @Test
    void testLateSubCommandsAreGraftedOntoTheCommandMapNode() {
        givenCommandMapNode(TOP_LABEL);
        addSubCommand("go");
        registrar.registerCommand(topCommand);

        addSubCommand("deathchest");
        registrar.refreshTrees();

        assertNotNull(dispatcher.getRoot().getChild(TOP_LABEL).getChild("deathchest"));
    }

    /**
     * Brigadier merges by name and drops the redirect while doing it, so a redirect
     * registered over an existing node would replace that node's executor with ours
     * and give nothing back. The command map's own node dispatches the alias
     * perfectly well, so it is left alone.
     */
    @Test
    void testAnAliasHeldByTheCommandMapIsLeftAlone() {
        givenCommandMapNode(TOP_LABEL);
        CommandNode<CommandSourceStack> alias = givenCommandMapNode(ALIAS);
        addSubCommand("go");

        registrar.registerCommand(topCommand);

        assertSame(alias, dispatcher.getRoot().getChild(ALIAS));
        assertNull(dispatcher.getRoot().getChild(ALIAS).getRedirect(),
                "An alias that already has a node must not be taken over");
    }

    /**
     * Without a command map - the legacy path, and every other server - the alias
     * still gets a redirect onto the main node, which must be the node that is
     * actually in the tree.
     */
    @Test
    void testAnUnclaimedAliasStillRedirectsToTheLiveNode() {
        addSubCommand("go");

        registrar.registerCommand(topCommand);

        CommandNode<CommandSourceStack> alias = dispatcher.getRoot().getChild(ALIAS);
        assertNotNull(alias);
        assertSame(dispatcher.getRoot().getChild(TOP_LABEL), alias.getRedirect());
    }

    /**
     * A reload takes the commands out of the command map, which on Paper takes the
     * nodes with them. Registration has to start from scratch afterwards or the
     * command comes back with no sub-command tree at all.
     */
    @Test
    void testResetLetsACommandBeRegisteredAgain() throws Exception {
        givenCommandMapNode(TOP_LABEL);
        addSubCommand("go");
        registrar.registerCommand(topCommand);

        // The command map removal that goes with a reload takes the node with it
        injectDispatcher(new CommandDispatcher<>());
        registrar.reset();
        givenCommandMapNode(TOP_LABEL);
        registrar.registerCommand(topCommand);

        assertNotNull(dispatcher.getRoot().getChild(TOP_LABEL).getChild("go"),
                "A command registered again after a reload must get its tree back");
    }

    @Test
    void testRepeatedRefreshesDoNotDuplicateNodes() {
        givenCommandMapNode(TOP_LABEL);
        addSubCommand("go");
        registrar.registerCommand(topCommand);
        int children = dispatcher.getRoot().getChild(TOP_LABEL).getChildren().size();

        registrar.refreshTrees();
        registrar.refreshTrees();

        assertEquals(children, dispatcher.getRoot().getChild(TOP_LABEL).getChildren().size());
    }
}
