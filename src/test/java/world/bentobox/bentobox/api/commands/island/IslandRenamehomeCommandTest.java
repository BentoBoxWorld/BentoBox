package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.prompts.ChatInputListener;
import world.bentobox.bentobox.api.prompts.PromptTestHelper;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Tests for {@link IslandRenamehomeCommand}. The new name is asked for with a prompt, which
 * these tests answer in chat.
 */
class IslandRenamehomeCommandTest extends RanksManagerTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;

    private IslandRenamehomeCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        Settings settings = new Settings();
        // Ask in chat: a dialog cannot be built without a server
        settings.setDialogPrompts(false);
        when(plugin.getSettings()).thenReturn(settings);

        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(anyString())).thenReturn(Optional.empty());
        GameModeAddon addon = mock(GameModeAddon.class);
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getWorld()).thenReturn(world);

        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.isPlayer()).thenReturn(true);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer(i -> i.getArgument(0, String.class));

        Map<String, Location> homes = new HashMap<>();
        homes.put("base", null);
        homes.put("farm", null);
        when(island.getHomes()).thenReturn(homes);
        when(island.getRank(user)).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);
        when(island.renameHome(anyString(), anyString())).thenReturn(true);
        when(im.getIsland(world, user)).thenReturn(island);
        when(im.isHomeLocation(any(), anyString())).thenReturn(true);

        cmd = new IslandRenamehomeCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        PromptTestHelper.clear(uuid);
        super.tearDown();
    }

    private void run() {
        assertTrue(cmd.canExecute(user, "renamehome", List.of("base")));
        assertTrue(cmd.execute(user, "renamehome", List.of("base")));
    }

    @Test
    void testNoArgsShowsHelp() {
        assertFalse(cmd.canExecute(user, "renamehome", List.of()));
    }

    @Test
    void testUnknownHome() {
        when(im.isHomeLocation(any(), anyString())).thenReturn(false);
        assertFalse(cmd.canExecute(user, "renamehome", List.of("nowhere")));
        verify(user).sendMessage("commands.island.go.unknown-home");
    }

    @Test
    void testAsksThenRenames() {
        run();
        assertTrue(ChatInputListener.isWaitingFor(uuid));
        verify(island, never()).renameHome(anyString(), anyString());

        PromptTestHelper.say(uuid, "Mine");

        verify(island).renameHome("base", "Mine");
        verify(user).sendMessage("general.success");
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }

    @Test
    void testNameInUseIsAskedForAgain() {
        run();

        PromptTestHelper.say(uuid, "FARM");

        verify(island, never()).renameHome(anyString(), anyString());
        verify(user).sendRawMessage("commands.island.renamehome.already-exists");
        assertTrue(ChatInputListener.isWaitingFor(uuid));

        PromptTestHelper.say(uuid, "Mine");
        verify(island).renameHome("base", "Mine");
    }

    @Test
    void testCancel() {
        run();

        PromptTestHelper.say(uuid, "prompts.chat.cancel-word");

        verify(island, never()).renameHome(anyString(), anyString());
        assertFalse(ChatInputListener.isWaitingFor(uuid));
    }
}
