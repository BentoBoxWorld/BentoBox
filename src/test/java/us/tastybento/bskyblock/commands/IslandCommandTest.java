package us.tastybento.bskyblock.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.IslandEventBuilder;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.managers.island.NewIsland;
import us.tastybento.bskyblock.managers.island.NewIsland.Builder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, NewIsland.class, IslandEvent.class })
public class IslandCommandTest {

    @Mock
    static BSkyBlock plugin;
    private static World world;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Server server = mock(Server.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("BSkyBlock_test_world");
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);

        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        
    }
    
    @Before
    public void setup() {
        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getWorld(Mockito.anyString())).thenReturn(world);
        when(plugin.getIWM()).thenReturn(iwm);
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
    public void testExecuteUserListOfString() throws IOException {
        PowerMockito.mockStatic(NewIsland.class);
        Builder builder = mock(Builder.class);
        Island island = mock(Island.class);
        when(builder.build()).thenReturn(island);
        when(builder.oldIsland(Mockito.any())).thenReturn(builder);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.world(Mockito.any())).thenReturn(builder);
        when(NewIsland.builder()).thenReturn(builder);
        
        PowerMockito.mockStatic(IslandEvent.class);
        IslandEventBuilder ieb = mock(IslandEventBuilder.class);
        when(ieb.admin(Mockito.anyBoolean())).thenReturn(ieb);
        IslandBaseEvent event = mock(IslandBaseEvent.class);
        when(ieb.build()).thenReturn(event);
        when(ieb.involvedPlayer(Mockito.any())).thenReturn(ieb);
        when(ieb.island(Mockito.any())).thenReturn(ieb);
        when(ieb.location(Mockito.any())).thenReturn(ieb);
        when(ieb.reason(Mockito.any())).thenReturn(ieb);
        when(IslandEvent.builder()).thenReturn(ieb);
        
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Setup
        IslandCommand ic = new IslandCommand();
        assertFalse(ic.execute(null, null));
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(uuid);
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);

        // User has an island - so go there!
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertTrue(ic.execute(user, new ArrayList<>()));
        when(user.getWorld()).thenReturn(world);


        Location location = mock(Location.class);
        when(island.getCenter()).thenReturn(location);
        // No island yet, one will be created
        when(im.createIsland(Mockito.any(), Mockito.any())).thenReturn(island);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        assertTrue(ic.execute(user, new ArrayList<>()));
        
        // No such command
        String[] args2 = {"random", "junk"};
        assertFalse(ic.execute(user, Arrays.asList(args2)));
    }

}
