package bskyblock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent.TeamReason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class TestIslandCommand {
    private final UUID playerUUID = UUID.randomUUID();
    private CommandSender sender;
    private Player player;

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
        Mockito.when(server.getVersion()).thenReturn("BSB_Mocking");
        Bukkit.setServer(server);
        Mockito.when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        sender = mock(CommandSender.class);
        player = mock(Player.class);
        Mockito.when(player.hasPermission(Settings.PERMPREFIX + "default.permission")).thenReturn(true);


        //Mockito.doReturn(Logger.getAnonymousLogger()).when(plugin.getLogger());        
    }

    @Test
    public void test() {

        // Test island events
        IslandBaseEvent event = TeamEvent.builder()
                //.island(getIslands().getIsland(playerUUID))
                .reason(TeamReason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        assertEquals(playerUUID, event.getPlayerUUID());

        // Test command
        User user = User.getInstance(playerUUID);
        CompositeCommand testCommand = new TestCommand();
        testCommand.setOnlyPlayer(true);
        testCommand.setPermission(Settings.PERMPREFIX + "default.permission");
        // Test basic execution
        assertTrue(testCommand.execute(user, new ArrayList<>()));
        assertEquals("test",testCommand.getLabel());
        assertEquals(2, testCommand.getAliases().size());
        assertEquals("t", testCommand.getAliases().get(0));
        assertTrue(testCommand.isOnlyPlayer());
        assertEquals(testCommand.getParent(), Optional.empty());
        assertEquals(Settings.PERMPREFIX + "default.permission", testCommand.getPermission());
        // Check commands and aliases match to correct class
        for (Entry<String, CompositeCommand> command : testCommand.getSubCommands().entrySet()) {
            assertEquals(testCommand.getSubCommand(command.getKey()), command.getValue());
            // Check aliases
            for (String alias : command.getValue().getAliases()) {
                assertEquals(testCommand.getSubCommand(alias), command.getValue());
            }            
        }
        String[] args = {""};
        assertEquals(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(player, "test", args));
        assertNotSame(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(sender, "test", args));
        args[0] = "su";
        assertEquals(Arrays.asList("sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "d";
        assertNotSame(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "sub1";
        assertEquals(Arrays.asList(), testCommand.tabComplete(player, "test", args));
        String[] args2 = {"sub2",""};
        assertEquals(Arrays.asList("help", "subsub"), testCommand.tabComplete(player, "test", args2));
        args2[1] = "s";
        assertEquals(Arrays.asList("subsub"), testCommand.tabComplete(player, "test", args2));
        String[] args3 = {"sub2","subsub", ""};
        assertEquals(Arrays.asList("help", "subsubsub"), testCommand.tabComplete(player, "test", args3));
        // Test for overridden tabcomplete
        assertEquals(Arrays.asList(new String[] {"Florian", "Ben", "Bill", "Ted", "help"}),
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", ""}));
        // Test for partial word
        assertEquals(Arrays.asList(new String[] {"Ben", "Bill"}), 
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", "b"}));
        
        // Test command arguments
        CompositeCommand argCmd = new Test3ArgsCommand();
        argCmd.setOnlyPlayer(true);
        argCmd.setPermission(Settings.PERMPREFIX + "default.permission");
        assertTrue(argCmd.execute(player, "args", new String[]{"give", "100", "ben"}));        
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub"}));
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben"}));
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben", "100"}));
        assertTrue(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben", "100", "today"}));
        
        // Usage tests
        assertEquals("/test test.usage", testCommand.getUsage());
        assertEquals("/test test.usage", testCommand.getUsage("sdfsd"));
        assertEquals("/test sub1 sub.usage", testCommand.getUsage("sub1"));
        // If the usage has not been defined, then it just shows the command
        assertEquals("/test sub2 subsub", testCommand.getUsage("sub2", "subsub"));
        
        // Test help
        //assertTrue(testCommand.execute(player,  "test", new String[] {"help"}));
    }

    private class TestCommand extends CompositeCommand {

        public TestCommand() {
            super("test", "t", "tt");
            this.setUsage("test.usage");
        }

        @Override
        public boolean execute(User user, List<String> args) {
            return true;
        }

        @Override
        public void setup() {
            // Set up sub commands
            new TestSubCommand(this); // Level 1
            new TestSubCommand2(this); // Has sub command
        }

    }

    private class TestSubCommand extends CompositeCommand {

        public TestSubCommand(CompositeCommand parent) {
            super(parent, "sub1", "subone");
            this.setUsage("sub.usage");
        }

        @Override
        public boolean execute(User user, List<String> args) {
            return true;
        }

    }

    private class TestSubCommand2 extends CompositeCommand {

        public TestSubCommand2(CompositeCommand parent) {
            super(parent, "sub2", "level1");

        }

        @Override
        public boolean execute(User user, List<String> args) {
            return true;
        }

        @Override
        public void setup() {
            // Set up sub commands
            new TestSubSubCommand(this); // Level 2
        }
    }

    private class TestSubSubCommand extends CompositeCommand {

        public TestSubSubCommand(CompositeCommand parent) {
            super(parent, "subsub", "level2", "subsubby");

        }

        @Override
        public boolean execute(User user, List<String> args) {
            return true;
        }

        @Override
        public void setup() {
            // Set up sub commands
            new TestSubSubSubCommand(this); // Level 3
        }

    }

    private class TestSubSubSubCommand extends CompositeCommand {

        public TestSubSubSubCommand(CompositeCommand parent) {
            super(parent, "subsubsub", "level3", "subsubsubby");

        }

        @Override
        public boolean execute(User user, List<String> args) {
            Bukkit.getLogger().info("args are " + args.toString());
            if (args.size() == 3) return true;
            return false;
        }

        @Override
        public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
            List<String> options = new ArrayList<>();
            String lastArg = (!args.isEmpty() ? args.getLast() : "");
            options.addAll(Arrays.asList(new String[] {"Florian", "Ben", "Bill", "Ted"}));
            return Optional.of(Util.tabLimit(options, lastArg));
        }
    }
    
    private class Test3ArgsCommand extends CompositeCommand {

        public Test3ArgsCommand() {
            super("args", "");
        }

        @Override
        public boolean execute(User user, List<String> args) {
            Bukkit.getLogger().info("args are " + args.toString());
            if (args.size() == 3) return true;
            return false;
        }

   }
}
