package world.bentobox.bentobox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Flags.class, Util.class, Bukkit.class})
public class TestBentoBox extends AbstractCommonSetup {
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID VISITOR_UUID = UUID.randomUUID();
    @Mock
    private CommandSender sender;
    @Mock
    private Block block;
    @Mock
    private Player ownerOfIsland;
    @Mock
    private Player visitorToIsland;
    @Mock
    private CommandsManager cm;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(plugin.getCommandsManager()).thenReturn(cm);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(offlinePlayer);
        when(offlinePlayer.getName()).thenReturn("tastybento");

        when(player.hasPermission(anyString())).thenReturn(true);

        when(ownerOfIsland.getLocation()).thenReturn(location);
        when(visitorToIsland.getLocation()).thenReturn(location);
        when(location.clone()).thenReturn(location);

        when(player.getUniqueId()).thenReturn(MEMBER_UUID);
        when(ownerOfIsland.getUniqueId()).thenReturn(uuid);
        when(visitorToIsland.getUniqueId()).thenReturn(VISITOR_UUID);

        island.setOwner(uuid);
        island.setProtectionRange(100);
        HashMap<UUID, Integer> members = new HashMap<>();
        members.put(uuid, RanksManager.OWNER_RANK);
        members.put(MEMBER_UUID, RanksManager.MEMBER_RANK);
        island.setMembers(members);

    }

    @Test
    public void testIslandEvent() {
        // Test island events
        IslandBaseEvent event = TeamEvent.builder()
                //.island(getIslands().getIsland(playerUUID))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(uuid)
                .build();
        assertEquals(uuid, event.getPlayerUUID());
    }

    @Test
    public void testCommandAPI() {
        // Test command
        User user = User.getInstance(uuid);
        CompositeCommand testCommand = new TestCommand();
        testCommand.setOnlyPlayer(true);
        testCommand.setPermission("default.permission");
        // Test basic execution
        assertTrue(testCommand.execute(user, testCommand.getLabel(), new ArrayList<>()));
        assertEquals("test",testCommand.getLabel());
        assertEquals(2, testCommand.getAliases().size());
        assertEquals("t", testCommand.getAliases().get(0));
        assertTrue(testCommand.isOnlyPlayer());
        assertNull(testCommand.getParent());
        assertEquals("default.permission", testCommand.getPermission());
        // Check commands and aliases match to correct class
        for (Entry<String, CompositeCommand> command : testCommand.getSubCommands().entrySet()) {
            assertEquals(testCommand.getSubCommand(command.getKey()), Optional.of(command.getValue()));
            // Check aliases
            for (String alias : command.getValue().getAliases()) {
                assertEquals(testCommand.getSubCommand(alias), Optional.of(command.getValue()));
            }
        }
        String[] args = {""};
        // Results are alphabetically sorted
        when(Util.tabLimit(any(), any())).thenCallRealMethod();
        assertEquals(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(player, "test", args));
        assertNotSame(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(sender, "test", args));
        args[0] = "su";
        assertEquals(Arrays.asList("sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "d";
        assertNotSame(Arrays.asList("help", "sub1","sub2"), testCommand.tabComplete(player, "test", args));
        args[0] = "sub1";
        assertEquals(Collections.emptyList(), testCommand.tabComplete(player, "test", args));
        String[] args2 = {"sub2",""};
        assertEquals(Arrays.asList("help", "subsub"), testCommand.tabComplete(player, "test", args2));
        args2[1] = "s";
        assertEquals(Collections.singletonList("subsub"), testCommand.tabComplete(player, "test", args2));
        String[] args3 = {"sub2","subsub", ""};
        assertEquals(Arrays.asList("help", "subsubsub"), testCommand.tabComplete(player, "test", args3));
        // Test for overridden tabcomplete
        assertEquals(Arrays.asList("Ben", "Bill", "Florian", "Ted", "help"),
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", ""}));
        // Test for partial word
        assertEquals(Arrays.asList("Ben", "Bill"),
                testCommand.tabComplete(player, "test", new String[] {"sub2", "subsub", "subsubsub", "b"}));

        // Test command arguments
        CompositeCommand argCmd = new Test3ArgsCommand();
        argCmd.setOnlyPlayer(true);
        argCmd.setPermission("default.permission");
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
            super("test", "t", "tt");
            setParametersHelp("test.params");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
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
            setParametersHelp("sub.params");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

    }

    private class TestSubCommand2 extends CompositeCommand {

        public TestSubCommand2(CompositeCommand parent) {
            super(parent, "sub2", "level1");

        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
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
        public boolean execute(User user, String label, List<String> args) {
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
        public boolean execute(User user, String label, List<String> args) {

            return args.size() == 3;
        }

        @Override
        public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            List<String> options = new ArrayList<>(Arrays.asList("Florian", "Ben", "Bill", "Ted"));
            return Optional.of(Util.tabLimit(options, lastArg));
        }
    }

    private class Test3ArgsCommand extends CompositeCommand {

        public Test3ArgsCommand() {
            super("args", "");
        }

        @Override
        public void setup() {}

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return args.size() == 3;
        }

    }

    // Protection tests
    @Test
    public void testProtection() {
        User owner = User.getInstance(uuid);
        Island island = new Island();
        island.setOwner(uuid);
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

        Set<UUID> members = island.getMemberSet();
        assertTrue(members.contains(uuid));
        assertTrue(members.contains(member1));
        assertTrue(members.contains(member2));
        assertTrue(members.contains(member3));

        // Remove members
        island.removeMember(member3);
        members = island.getMemberSet();
        assertTrue(members.contains(uuid));
        assertTrue(members.contains(member1));
        assertTrue(members.contains(member2));
        assertFalse(members.contains(member3));

        // Ban member
        island.ban(UUID.randomUUID(), member1);
        members = island.getMemberSet();
        assertTrue(members.contains(uuid));
        assertFalse(members.contains(member1));
        assertTrue(members.contains(member2));
        assertFalse(members.contains(member3));

        Set<UUID> banned = island.getBanned();
        assertTrue(banned.contains(member1));

        // Unban
        island.unban(UUID.randomUUID(), member1);
        assertFalse(island.getBanned().contains(member1));

        // Protection

        // Check default settings
        // Owner should be able to do anything
        assertTrue(island.isAllowed(owner, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(owner, Flags.BREAK_BLOCKS));

        // Visitor can do nothing
        assertFalse(island.isAllowed(visitor, Flags.PLACE_BLOCKS));
        assertFalse(island.isAllowed(visitor, Flags.BREAK_BLOCKS));

        // Set up protection settings - members can break blocks, visitors and place blocks
        island.setFlag(Flags.BREAK_BLOCKS, RanksManager.MEMBER_RANK);
        assertFalse(island.isAllowed(visitor, Flags.BREAK_BLOCKS));

        island.setFlag(Flags.PLACE_BLOCKS, RanksManager.VISITOR_RANK);
        assertFalse(island.isAllowed(visitor, Flags.BREAK_BLOCKS));

        // Owner should be able to do anything
        assertTrue(island.isAllowed(owner, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(owner, Flags.BREAK_BLOCKS));

        // Visitor can only place blocks
        assertTrue(island.isAllowed(visitor, Flags.PLACE_BLOCKS));
        assertFalse(island.isAllowed(visitor, Flags.BREAK_BLOCKS));

        // Check if the members have capability
        User mem1 = User.getInstance(member1); // Visitor
        User mem2 = User.getInstance(member2); // Member
        island.ban(member2, member3);
        User mem3 = User.getInstance(member3); // Banned

        // Member 1 is a visitor
        assertTrue(island.isAllowed(mem1, Flags.PLACE_BLOCKS));
        assertFalse(island.isAllowed(mem1, Flags.BREAK_BLOCKS));

        // Member 2 is a team member
        assertTrue(island.isAllowed(mem2, Flags.PLACE_BLOCKS));
        assertTrue(island.isAllowed(mem2, Flags.BREAK_BLOCKS));

        // Member 3 is no longer a member and is banned
        assertFalse(island.isAllowed(mem3, Flags.PLACE_BLOCKS));
        assertFalse(island.isAllowed(mem3, Flags.BREAK_BLOCKS));
    }

    @Test
    public void testEventProtection() {
        // Test events

        TestFlagListener fl = new TestFlagListener(plugin);

        // checking events - vistor
        Event e3 = new BlockBreakEvent(block, visitorToIsland);
        Assert.assertFalse(fl.checkIsland(e3, visitorToIsland, location, Flags.BREAK_BLOCKS, true));

        // checking events - owner
        Event e = new BlockBreakEvent(block, ownerOfIsland);
        Assert.assertTrue(fl.checkIsland(e, ownerOfIsland, location, Flags.BREAK_BLOCKS, true));

        // checking events - member
        Event e2 = new BlockBreakEvent(block, player);
        Assert.assertTrue(fl.checkIsland(e2, player, location, Flags.BREAK_BLOCKS, true));

    }

    @Test
    public void testDefaultFlags() {
        // Check all the default flags
        FlagsManager fm = new FlagsManager(plugin);
        Collection<Flag> defaultFlags = Flags.values();
        Collection<Flag> f = fm.getFlags();
        for (Flag flag : defaultFlags) {
            assertTrue(flag.getID(), f.contains(flag));
        }
        for (Flag flag : f) {
            assertTrue(flag.getID(), defaultFlags.contains(flag));
        }
    }

    @Test
    public void testCustomFlags() {
        // Custom
        TestFlagListener fl = new TestFlagListener(plugin);
        Flag customFlag = new Flag.Builder("CUSTOM_FLAG", Material.DIAMOND).listener(fl).build();
        assertEquals("CUSTOM_FLAG", customFlag.getID());
        assertEquals(Material.DIAMOND, customFlag.getIcon());
        assertEquals(fl, customFlag.getListener().get());
        // Add it to the Flag Manager
        FlagsManager flagManager = new FlagsManager(plugin);
        flagManager.registerFlag(customFlag);
        assertEquals(customFlag, flagManager.getFlag("CUSTOM_FLAG").get());
    }

    /**
     * Dummy flag listener
     *
     */
    private class TestFlagListener extends FlagListener {
        TestFlagListener(BentoBox plugin) {
            // Set the plugin explicitly
            setPlugin(plugin);
        }
    }

}
