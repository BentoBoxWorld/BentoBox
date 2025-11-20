package world.bentobox.bentobox.api.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;

public class DefaultHelpCommandTest extends AbstractCommonSetup {

    private User user;

    @Override
   @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
         // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(mockPlayer);

        // Parent command has no aliases
        CompositeCommand ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

    }

    class FakeParent extends CompositeCommand {

        public FakeParent() {
            super("island", "is");
        }

        @Override
        public void setup() {
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return false;
        }

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for {@link DefaultHelpCommand}
     */
    @Test
    public void testSetup() {
        CompositeCommand cc = mock(CompositeCommand.class);
        DefaultHelpCommand dhc = new DefaultHelpCommand(cc);
        assertNotNull(dhc);
        // Verify that parent's parameters and permission is used
        verify(cc).getParameters();
        verify(cc).getDescription();
        verify(cc).getPermission();
    }

    /**
     * Test for {@link DefaultHelpCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteUserListOfString() {
        CompositeCommand parent = mock(CompositeCommand.class);
        when(parent.getLabel()).thenReturn("island");
        when(parent.getUsage()).thenReturn("island");
        when(parent.getParameters()).thenReturn("parameters");
        when(parent.getDescription()).thenReturn("description");
        when(parent.getPermission()).thenReturn("permission");
        when(parent.getWorld()).thenReturn(mock(World.class));
        when(user.getTranslationOrNothing("parameters")).thenReturn("");
        when(user.getTranslation("description")).thenReturn("the main island command");
        DefaultHelpCommand dhc = new DefaultHelpCommand(parent);
        dhc.execute(user, dhc.getLabel(), Collections.emptyList());
        verify(user).sendMessage("commands.help.header", "[label]", "BSkyBlock");
        verify(user).getTranslationOrNothing("parameters");
        verify(user).getTranslation("description");
        verify(user).sendMessage("commands.help.syntax-no-parameters", "[usage]", "island", "[description]",
                "the main island command");
        verify(user).sendMessage("commands.help.end");
    }

    /**
     * Test for {@link DefaultHelpCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteSecondLevelHelp() {
        CompositeCommand parent = mock(CompositeCommand.class);
        when(parent.getLabel()).thenReturn("island");
        when(parent.getUsage()).thenReturn("island");
        when(parent.getParameters()).thenReturn("parameters");
        when(parent.getDescription()).thenReturn("description");
        when(parent.getPermission()).thenReturn("permission");
        when(user.getTranslationOrNothing("parameters")).thenReturn("");
        when(user.getTranslation("description")).thenReturn("the main island command");
        DefaultHelpCommand dhc = new DefaultHelpCommand(parent);
        dhc.execute(user, dhc.getLabel(), Collections.singletonList("1"));
        // There are no header or footer shown
        verify(user).getTranslationOrNothing("parameters");
        verify(user).getTranslation("description");
        verify(user).sendMessage("commands.help.syntax-no-parameters", "[usage]", "island", "[description]",
                "the main island command");
    }

    /**
     * Test for {@link DefaultHelpCommand#execute(User, String, List)}
     */
    @Test
    public void testExecuteDirectHelpHelp() {
        CompositeCommand parent = mock(CompositeCommand.class);
        when(parent.getLabel()).thenReturn("island");
        when(parent.getUsage()).thenReturn("island");
        when(user.getTranslation("island")).thenReturn("island");
        when(user.getTranslationOrNothing("island")).thenReturn("island");
        when(user.getTranslation("commands.help.parameters")).thenReturn("help-parameters");
        when(user.getTranslationOrNothing("commands.help.parameters")).thenReturn("help-parameters");
        when(user.getTranslation("commands.help.description")).thenReturn("the help command");
        when(user.getTranslationOrNothing("commands.help.description")).thenReturn("the help command");
        DefaultHelpCommand dhc = new DefaultHelpCommand(parent);
        // Test /island help team
        dhc.execute(user, dhc.getLabel(), Collections.singletonList("team"));
        // There are no header or footer shown
        verify(user).getTranslation("commands.help.parameters");
        verify(user).getTranslation("commands.help.description");
        verify(user).sendMessage("commands.help.syntax", "[usage]", "island", "[parameters]", "help-parameters",
                "[description]", "the help command");
    }
}
