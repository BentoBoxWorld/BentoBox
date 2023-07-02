package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class DefaultPlayerCommandTest {

    @Mock
    GameModeAddon addon;
    private PlayerCommand dpc;
    @Mock
    private WorldSettings ws;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private @Nullable Island island;


    class PlayerCommand extends DefaultPlayerCommand {

        protected PlayerCommand(GameModeAddon addon) {
            super(addon);
        }

    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Addon

        // User
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        // IM
        when(plugin.getIslandsManager()).thenReturn(im);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(island);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // World Settings
        when(ws.getDefaultPlayerAction()).thenReturn("go");
        when(ws.getDefaultNewPlayerAction()).thenReturn("create");

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        when(ws.getPlayerCommandAliases()).thenReturn("island is");
        when(addon.getWorldSettings()).thenReturn(ws);
        dpc = new PlayerCommand(addon);
        dpc.setWorld(mock(World.class));

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#DefaultPlayerCommand(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testDefaultPlayerCommand() {
        assertNotNull(dpc);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("commands.island.help.description", dpc.getDescription());
        assertTrue(dpc.isOnlyPlayer());
        assertEquals("island", dpc.getPermission());
        // 20 = 19 subcommands + help command
        assertEquals(20, dpc.getSubCommands().size()); // Update when commands are added or removed
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownCommand() {
        assertFalse(dpc.execute(user, "label", List.of("unknown")));
        verify(user).sendMessage("general.errors.unknown-command", TextVariables.LABEL, "island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNullUser() {
        assertFalse(dpc.execute(null, "label", List.of()));
    }
    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasIsland() {
        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasNoIsland() {
        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(null);
        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasIslandUnknownCommand() {
        when(ws.getDefaultPlayerAction()).thenReturn("goxxx");

        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).performCommand("label goxxx");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasNoIslandUnknownCommand() {

        when(ws.getDefaultNewPlayerAction()).thenReturn("createxxx");

        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(null);
        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).performCommand("label createxxx");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasIslandUnknownCommandSlash() {
        when(ws.getDefaultPlayerAction()).thenReturn("/goxxx");

        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).performCommand("goxxx");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.DefaultPlayerCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgsHasNoIslandUnknownCommandSlash() {

        when(ws.getDefaultNewPlayerAction()).thenReturn("/createxxx");

        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(null);
        assertFalse(dpc.execute(user, "label", List.of()));
        verify(user).performCommand("createxxx");
    }
}