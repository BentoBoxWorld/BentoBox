package us.tastybento.bskyblock.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
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
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BSkyBlock.class)
public class IslandCommandTest {

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

    @Test
    public void testIslandCommand() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        assertNotNull(new IslandCommand());
        // Verify the command has been registered
        Mockito.verify(cm).registerCommand(Mockito.any());
    }

    @Test
    public void testSetup() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        IslandCommand ic = new IslandCommand();
        assertEquals("commands.island.help.description", ic.getDescription());
        assertTrue(ic.isOnlyPlayer());
        // Permission
        assertEquals(Constants.PERMPREFIX + "island", ic.getPermission());

    }

    @Test
    public void testExecuteUserListOfString() {
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Setup
        IslandCommand ic = new IslandCommand();
        assertFalse(ic.execute(null, null));
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);

        // User has an island - so go there!
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        assertTrue(ic.execute(user, new ArrayList<>()));

        // No island yet
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(false);
        assertTrue(ic.execute(user, new ArrayList<>()));
        
        // No such command
        String[] args2 = {"random", "junk"};
        assertFalse(ic.execute(user, Arrays.asList(args2)));
    }

}
