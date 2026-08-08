package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.commands.CompositeCommand;

/**
 * Tests that BentoBox commands reach the Bukkit command map.
 * <p>
 * Brigadier registration replaced the command map registration outright, and that
 * took BentoBox commands out of the map every other plugin dispatches through:
 * NPC plugins, command signs and GUI plugins were answered with the server's
 * no-permission message instead of running the command, because Paper wraps a
 * command it cannot find there as a {@code VanillaCommandWrapper} demanding
 * {@code minecraft.command.<label>}. See issue #3050.
 *
 * @author tastybento
 */
class CommandsManagerTest extends CommonTestSetup {

    @Mock
    private LifecycleEventManager<Plugin> lifecycleManager;
    @Mock
    private Addon addon;

    private CommandsManager cm;
    private CompositeCommand command;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Without this the registrar cannot hook Paper and quietly falls back to
        // the legacy path, which would make these tests prove nothing
        when(plugin.getLifecycleManager()).thenReturn(lifecycleManager);
        assertTrue(plugin.getSettings().isUseBrigadierCommands(), "Brigadier registration is on by default");

        AddonDescription description = mock(AddonDescription.class);
        when(description.getName()).thenReturn("ChunkBlock");
        when(addon.getDescription()).thenReturn(description);

        cm = new CommandsManager();
        command = mock(CompositeCommand.class);
        when(command.getLabel()).thenReturn("ai");
        when(command.getName()).thenReturn("ai");
        when(command.getAliases()).thenReturn(new ArrayList<>(List.of("island")));
        when(command.getSubCommands()).thenReturn(new LinkedHashMap<>());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testRegisteredCommandIsInTheCommandMap() {
        cm.registerCommand(command);

        assertSame(command, server.getCommandMap().getCommand("ai"),
                "Anything dispatching through the command map has to find the command itself");
    }

    @Test
    void testAliasesAreInTheCommandMap() {
        cm.registerCommand(command);

        assertSame(command, server.getCommandMap().getCommand("island"));
    }

    @Test
    void testTheNamespacedFormIsInTheCommandMap() {
        cm.registerCommand(command);

        assertSame(command, server.getCommandMap().getCommand("bentobox:ai"));
    }

    @Test
    void testUnregisterTakesCommandsOutOfTheCommandMap() {
        cm.registerCommand(command);

        cm.unregisterCommands();

        assertNull(server.getCommandMap().getCommand("ai"));
        assertTrue(cm.getCommands().isEmpty());
    }

    @Test
    void testCommandsComeBackAfterAReload() {
        cm.registerCommand(command);
        cm.unregisterCommands();

        cm.registerCommand(command);

        assertSame(command, server.getCommandMap().getCommand("ai"));
    }

    @Test
    void testUnregisterWithoutRegisteringIsSafe() {
        cm.unregisterCommands();

        assertTrue(cm.getCommands().isEmpty());
    }

    /**
     * An addon that never enables must not leave its commands behind. A game mode's
     * commands are only given their world when the addon enables, so a leftover
     * command throws for every player who runs it - see #3059.
     */
    @Test
    void testUnregisterByAddonTakesThatAddonsCommandsOutOfTheCommandMap() {
        when(command.<Addon>getAddon()).thenReturn(addon);
        cm.registerCommand(command);
        assertSame(command, server.getCommandMap().getCommand("ai"), "Precondition: the command was registered");

        cm.unregisterCommands(addon);

        assertNull(server.getCommandMap().getCommand("ai"), "The command must not still be runnable");
        assertNull(cm.getCommand("ai"), "A leftover entry would keep the Brigadier node alive");
    }

    @Test
    void testUnregisterByAddonLeavesOtherAddonsAlone() {
        when(command.<Addon>getAddon()).thenReturn(addon);
        CompositeCommand other = mock(CompositeCommand.class);
        when(other.getLabel()).thenReturn("bsb");
        when(other.getName()).thenReturn("bsb");
        when(other.getAliases()).thenReturn(new ArrayList<>());
        when(other.getSubCommands()).thenReturn(new LinkedHashMap<>());
        // Built outside the when() - stubbing a mock inside another stubbing call
        // trips Mockito's unfinished stubbing check
        Addon otherAddon = namedAddon("BSkyBlock");
        when(other.<Addon>getAddon()).thenReturn(otherAddon);
        cm.registerCommand(command);
        cm.registerCommand(other);

        cm.unregisterCommands(addon);

        assertNull(server.getCommandMap().getCommand("ai"));
        assertSame(other, server.getCommandMap().getCommand("bsb"), "Only the failed addon's commands go");
    }

    @Test
    void testUnregisterByAddonWithNoCommandsIsSafe() {
        when(command.<Addon>getAddon()).thenReturn(addon);
        cm.registerCommand(command);

        cm.unregisterCommands(namedAddon("Level"));

        assertSame(command, server.getCommandMap().getCommand("ai"));
    }

    /**
     * An addon mock complete enough to be registered with: the command map prefix
     * comes from the addon's description.
     */
    private static Addon namedAddon(String name) {
        Addon a = mock(Addon.class);
        AddonDescription description = mock(AddonDescription.class);
        when(description.getName()).thenReturn(name);
        when(a.getDescription()).thenReturn(description);
        return a;
    }
}
