/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class CustomIslandMultiHomeHelpTest {

    @Mock
    private User user;
    private CustomIslandMultiHomeHelp ch;

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
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);

        // Parent command has no aliases
        CompositeCommand ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermission()).thenReturn("permission");
        when(ic.getUsage()).thenReturn("");


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

        // Command
        ch = new CustomIslandMultiHomeHelp(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#CustomIslandMultiHomeHelp(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testCustomIslandMultiHomeHelp() {
        assertEquals("help", ch.getLabel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(ch.isOnlyPlayer());
        assertEquals("parameters", ch.getParameters());
        assertEquals("description", ch.getDescription());
        assertEquals("permission", ch.getPermission());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringNotPlayer() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(ch.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringNoPerm() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        assertFalse(ch.canExecute(user, "", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertTrue(ch.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage(
                "commands.help.syntax",
                "[usage]",
                "",
                "[parameters]",
                "",
                "[description]",
                "description"
                );
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.CustomIslandMultiHomeHelp#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringMaxHomes() {
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(20);
        assertTrue(ch.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage(
                "commands.help.syntax",
                "[usage]",
                "",
                "[parameters]",
                "parameters",
                "[description]",
                "description"
                );
    }

}
