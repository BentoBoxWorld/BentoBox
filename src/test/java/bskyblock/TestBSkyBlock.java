package bskyblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.RanksManager;
import us.tastybento.bskyblock.util.Util;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

//@RunWith(PowerMockRunner.class)
//@SuppressStaticInitializationFor("us.tastybento.BSkyBlock")
//@PrepareForTest( { BSkyBlock.class })
public class TestBSkyBlock {
    private final UUID playerUUID = UUID.randomUUID();
    private static CommandSender sender;
    private static Player player;
    private static Location location;
    private static BSkyBlock plugin;
    private static FlagsManager flagsManager;

    @BeforeClass
    public static void setUp() {
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
        Mockito.when(player.hasPermission(Constants.PERMPREFIX + "default.permission")).thenReturn(true);

        plugin = mock(BSkyBlock.class);
        //Mockito.when(plugin.getServer()).thenReturn(server);

        location = mock(Location.class);
        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getBlockX()).thenReturn(0);
        Mockito.when(location.getBlockY()).thenReturn(0);
        Mockito.when(location.getBlockZ()).thenReturn(0);

        // This doesn't work!
        /*
        mockStatic(Bukkit.class);
        ItemFactory itemFactory = PowerMockito.mock(ItemFactory.class);
        PowerMockito.when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        PowerMockito.when(itemFactory.getItemMeta(any())).thenReturn(PowerMockito.mock(ItemMeta.class));

        mockStatic(BSkyBlock.class);
        flagsManager = mock(FlagsManager.class);
        PowerMockito.when(BSkyBlock.getInstance()).thenReturn(plugin);
        Mockito.when(plugin.getFlagsManager()).thenReturn(flagsManager);
        */
    }

    @Test
    public void testIslandEvent() {

        // Test island events
        IslandBaseEvent event = TeamEvent.builder()
                //.island(getIslands().getIsland(playerUUID))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(playerUUID)
                .build();
        assertEquals(playerUUID, event.getPlayerUUID());
    }
    
    @Test
    public void testCommandAPI() {
        // Test command
        User user = User.getInstance(playerUUID);
        CompositeCommand testCommand = new TestCommand();
        testCommand.setOnlyPlayer(true);
        testCommand.setPermission(Constants.PERMPREFIX + "default.permission");
        // Test basic execution
        assertTrue(testCommand.execute(user, new ArrayList<>()));
        assertEquals("test",testCommand.getLabel());
        assertEquals(2, testCommand.getAliases().size());
        assertEquals("t", testCommand.getAliases().get(0));
        assertTrue(testCommand.isOnlyPlayer());
        assertNull(testCommand.getParent());
        assertEquals(Constants.PERMPREFIX + "default.permission", testCommand.getPermission());
        // Check commands and aliases match to correct class
        for (Entry<String, CompositeCommand> command : testCommand.getSubCommands().entrySet()) {
            assertEquals(testCommand.getSubCommand(command.getKey()), Optional.of(command.getValue()));
            // Check aliases
            for (String alias : command.getValue().getAliases()) {
                assertEquals(testCommand.getSubCommand(alias), Optional.of(command.getValue()));
            }            
        }
        String[] args = {""};
        assertEquals(Arrays.asList("sub1","sub2", "help"), testCommand.tabComplete(player, "test", args));
        assertNotSame(Arrays.asList("sub1","sub2", "help"), testCommand.tabComplete(sender, "test", args));
        args[0] = "su";
        assertEquals(Arrays.asList("sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "d";
        assertNotSame(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "sub1";
        assertEquals(Arrays.asList(), testCommand.tabComplete(player, "test", args));
        String[] args2 = {"sub2",""};
        assertEquals(Arrays.asList("subsub", "help"), testCommand.tabComplete(player, "test", args2));
        args2[1] = "s";
        assertEquals(Arrays.asList("subsub"), testCommand.tabComplete(player, "test", args2));
        String[] args3 = {"sub2","subsub", ""};
        assertEquals(Arrays.asList("subsubsub", "help"), testCommand.tabComplete(player, "test", args3));
        // Test for overridden tabcomplete
        assertEquals(Arrays.asList(new String[] {"Florian", "Ben", "Bill", "Ted", "help"}),
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", ""}));
        // Test for partial word
        assertEquals(Arrays.asList(new String[] {"Ben", "Bill"}), 
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", "b"}));

        // Test command arguments
        CompositeCommand argCmd = new Test3ArgsCommand();
        argCmd.setOnlyPlayer(true);
        argCmd.setPermission(Constants.PERMPREFIX + "default.permission");
        assertTrue(argCmd.execute(player, "args", new String[]{"give", "100", "ben"}));        
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub"}));
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben"}));
        assertFalse(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben", "100"}));
        assertTrue(testCommand.execute(player,  "test", new String[] {"sub2", "subsub", "subsubsub", "ben", "100", "today"}));
        
        // Usage tests
        assertEquals("/test", testCommand.getUsage());
        assertEquals("test.params", testCommand.getParameters());
        
        // Test help
        //assertTrue(testCommand.execute(player,  "test", new String[] {"help"}));
    }

    private class TestCommand extends CompositeCommand {

        public TestCommand() {
            super(plugin, "test", "t", "tt");
            this.setParameters("test.params");
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
        }
        
        @Override
        public void setup() {
            this.setParameters("sub.params");
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
        public void setup() {}

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
            super(plugin, "args", "");
        }
        
        @Override
        public void setup() {}

        @Override
        public boolean execute(User user, List<String> args) {
            Bukkit.getLogger().info("args are " + args.toString());
            return args.size() == 3 ? true : false;
        }

   }
    
    // Protection tests
    @Test
    public void TestProtection() {
        User owner = User.getInstance(playerUUID);
        Island island = new Island();
        island.setOwner(playerUUID);
        island.setCenter(location);
        island.setProtectionRange(100);
        
        assertNotNull(island);
        
        User visitor = User.getInstance(UUID.randomUUID());
        assertEquals(RanksManager.OWNER_RANK, island.getRank(owner));
        assertEquals(RanksManager.VISITOR_RANK, island.getRank(visitor));
        
        // Make members
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID member3 = UUID.randomUUID();
        
        // Add members
        island.addMember(member1);
        island.addMember(member2);
        island.addMember(member3);
        
        Set<UUID> members = island.getMembers();
        assertTrue(members.contains(playerUUID));
        assertTrue(members.contains(member1));
        assertTrue(members.contains(member2));
        assertTrue(members.contains(member3));
        
        // Remove members
        island.removeMember(member3);
        members = island.getMembers();
        assertTrue(members.contains(playerUUID));
        assertTrue(members.contains(member1));
        assertTrue(members.contains(member2));
        assertFalse(members.contains(member3));
        
        // Ban member
        island.addToBanList(member1);
        members = island.getMembers();
        assertTrue(members.contains(playerUUID));
        assertFalse(members.contains(member1));
        assertTrue(members.contains(member2));
        assertFalse(members.contains(member3));
        
        Set<UUID> banned = island.getBanned();
        assertTrue(banned.contains(member1));
        
        // Unban
        island.removeFromBanList(member1);
        assertFalse(island.getBanned().contains(member1));
        
        // Protection
        // Set up protection settings - members can break blocks, visitors and place blocks
        // These tests do not work because of static method calls in the code and Bukkit.
        /*
        island.setFlag(Flags.BREAK_BLOCKS, RanksManager.MEMBER_RANK);
        island.setFlag(Flags.PLACE_BLOCKS, RanksManager.VISITOR_RANK);

        // Owner should be able to do anything
        assertTrue(island.isAllowed(owner, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(owner, Flags.BREAK_BLOCKS));

        // Visitor can only place blocks
        assertTrue(island.isAllowed(visitor, Flags.PLACE_BLOCKS));
        assertFalse(island.isAllowed(visitor, Flags.BREAK_BLOCKS));

        // Check if the members have capability
        User mem1 = User.getInstance(member1);
        User mem2 = User.getInstance(member2);
        User mem3 = User.getInstance(member3);

        assertTrue(island.isAllowed(mem1, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(mem1, Flags.BREAK_BLOCKS));

        assertTrue(island.isAllowed(mem2, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(mem2, Flags.BREAK_BLOCKS));

        // Member 3 is no longer a member and is a visitor
        assertTrue(island.isAllowed(mem3, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(mem3, Flags.BREAK_BLOCKS));

*/
        /*
         * 
         * Score approach:
         * 
         * Rank definitions are global and apply to all islands
         * 
         * There are 4 hard-coded ranks:
         * 
         * Owner is the highest rank = 1000
         * 
         * Member ranks are >= 900
         * 
         * Visitors = 0
         * 
         * Banned = -1
         * 
         * Owners have full admin capability over the island. Members are required to give up their own island to be a member.
         * Visitors are everyone else.
         * 
         * After those 3, it's possible to have custom ranks, e.g.
         * 
         * Trustees = 750 
         * Coops = 500
         * etc.
         *
         * 
         * Each flag has a bypass score.
         * If the user's rank is higher or equal to the bypass score, they will bypass the protection.
         * Owners can disable/enable the flags.
         * 
         * Each island will track the rank score for each player on the island.
         * Unknown players have a rank of 0.
         * 
         * 
         * Admins will be able to define groups and their rank value. 
         * During the game, the players will never see the rank value. They will only see the ranks.
         * 
         * It will be possible to island owners to promote or demote players up and down the ranks.
         * 
         * This will replace the team system completely.
         * 
         * Pros:
         * Very flexible
         * 
         * Cons:
         * Too complicated. Are there really ever going to be more than just a few ranks?
         * To have generic, unlimited ranks, we lose the concept of hard-coded teams, coops, etc.
         * The problem is that team members must lose their islands and so we have special code around that. 
         * i.e., there's a lot more going on than just ranks.
         * 
         * 
         * Permissions-based
         * 
         * 
         */
    }
}
