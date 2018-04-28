/**
 * 
 */
package us.tastybento.bskyblock.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.managers.CommandsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { BSkyBlock.class })
public class AdminCommandTest {

    @Mock
    static BSkyBlock plugin;
    private static World world;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
    }
    
    /**
     * Test method for {@link us.tastybento.bskyblock.commands.AdminCommand#AdminCommand()}.
     */
    @Test
    public void testAdminCommand() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        assertNotNull(new AdminCommand());
        // Verify the command has been registered
        Mockito.verify(cm).registerCommand(Mockito.any());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.AdminCommand#setup()}.
     */
    @Test
    public void testSetup() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        AdminCommand ac = new AdminCommand();
        ac.setup();
        assertEquals(Constants.PERMPREFIX + "admin.*", ac.getPermission());
        assertFalse(ac.isOnlyPlayer());
        assertEquals("commands.admin.help.parameters", ac.getParameters());
        assertEquals("commands.admin.help.description", ac.getDescription());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.AdminCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    @Test
    public void testExecuteUserListOfString() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        AdminCommand ac = new AdminCommand();
        assertTrue(ac.execute(mock(User.class), new ArrayList<>()));
        
        // No such command
        String[] args2 = {"random", "junk"};
        assertFalse(ac.execute(mock(User.class), Arrays.asList(args2)));
    }

}
