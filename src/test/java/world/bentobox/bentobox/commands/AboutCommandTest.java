package world.bentobox.bentobox.commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class AboutCommandTest {

    private User user;
    private CompositeCommand parent;

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
        parent = mock(CompositeCommand.class);
        when(parent.getSubCommandAliases()).thenReturn(new HashMap<>());
    }

    // FIXME how to avoid NPE with getDescription() ?
    /* @Test
    public void testExecute() {
        AboutCommand ac = new AboutCommand(parent);
        assertTrue(ac.execute(user, ac.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendRawMessage("Copyright (c) 2017 - 2018 Tastybento, Poslovitch");
        Mockito.verify(user).sendRawMessage("See https://www.eclipse.org/legal/epl-2.0/ for license information.");
    } */
}
