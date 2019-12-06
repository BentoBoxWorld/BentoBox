package world.bentobox.bentobox.api.commands;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.events.command.CommandEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, CommandEvent.class})
public class DefaultHelpCommandTest {

    private User user;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player player = mock(Player.class);
        // Sometimes use: Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);

        // Parent command has no aliases
        CompositeCommand ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());


        // No island for player to begin with (set it later in the tests)
        IslandsManager im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);


        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

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

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
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
        Mockito.verify(cc).getParameters();
        Mockito.verify(cc).getDescription();
        Mockito.verify(cc).getPermission();
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
        Mockito.verify(user).sendMessage("commands.help.header", "[label]", "BSkyBlock");
        Mockito.verify(user).getTranslationOrNothing("parameters");
        Mockito.verify(user).getTranslation("description");
        Mockito.verify(user).sendMessage(
                "commands.help.syntax-no-parameters",
                "[usage]",
                "island",
                "[description]",
                "the main island command"
                );
        Mockito.verify(user).sendMessage("commands.help.end");
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
        Mockito.verify(user).getTranslationOrNothing("parameters");
        Mockito.verify(user).getTranslation("description");
        Mockito.verify(user).sendMessage(
                "commands.help.syntax-no-parameters",
                "[usage]",
                "island",
                "[description]",
                "the main island command"
                );
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
        Mockito.verify(user).getTranslation("commands.help.parameters");
        Mockito.verify(user).getTranslation("commands.help.description");
        Mockito.verify(user).sendMessage(
                "commands.help.syntax",
                "[usage]",
                "island",
                "[parameters]",
                "help-parameters",
                "[description]",
                "the help command"
                );
    }
}
