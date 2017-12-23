package bskyblock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.config.Settings;

public class TestIslandCommand {
    private final UUID playerUUID = UUID.randomUUID();

    @Before
    public void setUp() {

        //Plugin plugin = mock(Plugin.class);
        //Mockito.doReturn(plugin).when(BSkyBlock.getPlugin());
        //Mockito.when().thenReturn(plugin);
        World world = mock(World.class);


        //Mockito.when(world.getWorldFolder()).thenReturn(worldFile);

        Server server = mock(Server.class);
        Mockito.when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Mockito.when(server.getWorld("world")).thenReturn(world);
        Mockito.when(server.getVersion()).thenReturn("TestTestMocking");
        Mockito.when(server.getVersion()).thenReturn("TestTestMocking");
        Bukkit.setServer(server);
        Mockito.when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        
        //Mockito.doReturn(Logger.getAnonymousLogger()).when(plugin.getLogger());        
    }

    @Test
    public void command() {
        IslandCommand islandCommand = new IslandCommand();
        User user = User.getInstance(playerUUID);
        // Test basic execution
        assertEquals(islandCommand.execute(user, null), true);
        assertEquals(islandCommand.getLabel(), Settings.ISLANDCOMMAND);
        assertEquals(islandCommand.getAliases().size(), 1);
        assertEquals(islandCommand.getAliases().get(0), "is");
        assertEquals(islandCommand.isOnlyPlayer(), true);
        assertEquals(islandCommand.getParent(), null);
        //TODO: assertEquals(islandCommand.getPermission(), "");
        // Check commands and aliases match to correct class
        for (Entry<String, CompositeCommand> command : islandCommand.getSubCommands().entrySet()) {
            assertEquals(islandCommand.getSubCommand(command.getKey()), command.getValue());
            // Check aliases
            for (String alias : command.getValue().getAliases()) {
                assertEquals(islandCommand.getSubCommand(alias), command.getValue());
            }            
        }
    }
    
}
