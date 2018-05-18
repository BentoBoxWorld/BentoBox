/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class })
public class IslandBanCommandTest {

    private BSkyBlock plugin;
    private IslandCommand ic;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(s.getResetLimit()).thenReturn(3);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");

        // Parent command has no aliases
        ic = mock(IslandCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.commands.island.IslandBanCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    // Island ban command by itself

    // *** Error conditions ***
    // Ban without an island
    // Ban as not a team leader
    // Ban unknown user
    // Ban self
    // Ban team mate
    // Ban someone you have already banned
    // Ban an Op

    // *** Working conditions ***
    // Ban offline user
    // Ban online user

    @Test
    public void testNoArgs() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        assertFalse(ibc.execute(user, new ArrayList<>()));
    }
    
    @Test
    public void testNoIsland() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testNotOwner() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.not-leader");
    }

    @Test
    public void testUnknownUser() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(null);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.unknown-player");
    }

    @Test
    public void testBanSelf() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.cannot-ban-yourself");
    }

    @Test
    public void testBanTeamMate() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID teamMate = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(teamMate);
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(teamMate);
        when(im.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.cannot-ban-member");
    }

    @Test
    public void testBanAlreadyBanned() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID bannedUser = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(bannedUser);
        when(island.isBanned(Mockito.eq(bannedUser))).thenReturn(true);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.player-already-banned");
    }

    @Test
    public void testBanOp() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID op = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(op);
        PowerMockito.mockStatic(User.class);
        User opUser = mock(User.class);
        when(opUser.isOp()).thenReturn(true);
        when(opUser.isPlayer()).thenReturn(true);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(opUser);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.cannot-ban");
    }

    @Test
    public void testBanOfflineUser() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID targetUuid = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUuid);        
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(targetUser);
        
        // Allow adding to ban list
        when(island.addToBanList(Mockito.any())).thenReturn(true);
        
        assertTrue(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(targetUser).sendMessage("commands.island.ban.owner-banned-you", "[owner]", user.getName());
    }

    @Test
    public void testBanOnlineUser() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID op = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(op);
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(true);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(targetUser);
        // Allow adding to ban list
        when(island.addToBanList(Mockito.any())).thenReturn(true);

        assertTrue(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(targetUser).sendMessage("commands.island.ban.owner-banned-you", "[owner]", user.getName());
    }
    
    @Test
    public void testCancelledBan() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID op = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(op);
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(true);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(targetUser);
        // Disallow adding to ban list - even cancelled
        when(island.addToBanList(Mockito.any())).thenReturn(false);

        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user, Mockito.never()).sendMessage("general.success");
        Mockito.verify(targetUser, Mockito.never()).sendMessage("commands.island.ban.owner-banned-you", "[owner]", user.getName());
    }
    
    @Test
    public void testTabComplete() {
        
        String[] names = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};
        Map<UUID, String> online = new HashMap<>();
        
        Set<UUID> banned = new HashSet<>();
        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < names.length; j++) {
            Player p = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p.getUniqueId()).thenReturn(uuid);
            when(p.getName()).thenReturn(names[j]);
            online.put(uuid, names[j]);
            // Ban the first 3 players
            if (j < 3) {
                banned.add(uuid);
            }
            onlinePlayers.add(p);
        }
        
        when(island.isBanned(Mockito.any(UUID.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return banned.contains(invocation.getArgumentAt(0, UUID.class));
            }
            
        });
        // Create the names
        when(pm.getName(Mockito.any(UUID.class))).then(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return online.getOrDefault(invocation.getArgumentAt(0, UUID.class), "tastybento");
            }
            
        });
        
        // Return a set of online players
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOnlinePlayers()).then(new Answer<Set<Player>>() {

            @Override
            public Set<Player> answer(InvocationOnMock invocation) throws Throwable {
                return onlinePlayers;
            }
            
        });
        
        IslandBanCommand ibc = new IslandBanCommand(ic);
        // Set up the user
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Player player = mock(Player.class);
        // Player can see every other player except Ian
        when(player.canSee(Mockito.any(Player.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Player p = invocation.getArgumentAt(0, Player.class);
                return p.getName().equals("ian") ? false : true;
            }

        });
        when(user.getPlayer()).thenReturn(player);
        
        // Get the tab-complete list with no argument
        Optional<List<String>> result = ibc.tabComplete(user, "", new LinkedList<>());
        assertFalse(result.isPresent());
        
        // Get the tab-complete list with one argument
        LinkedList<String> args = new LinkedList<>();
        args.add("");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        List<String> r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expectedNames = {"dave", "ed", "frank", "freddy", "george", "harry", "joe"};
        assertTrue(Arrays.equals(expectedNames, r.toArray()));
        
        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("d");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expectedName = {"dave"};
        assertTrue(Arrays.equals(expectedName, r.toArray()));
        
        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("fr");
        result = ibc.tabComplete(user, "", args);
        assertTrue(result.isPresent());
        r = result.get().stream().sorted().collect(Collectors.toList());
        // Compare the expected with the actual
        String[] expected = {"frank", "freddy"};
        assertTrue(Arrays.equals(expected, r.toArray()));
    }
}
