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
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(pm.inTeam(Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        when(im.getIsland(Mockito.any(UUID.class))).thenReturn(island);

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
    // Unban someone not banned
    // Ban an Op

    // *** Working conditions ***
    // Ban offline user
    // Ban online user
    // Unban offline user
    // Unban online user

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
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.not-leader");
    }

    @Test
    public void testUnknownUser() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(null);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.unknown-player");
    }

    @Test
    public void testBanSelf() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.cannot-ban-yourself");
    }

    @Test
    public void testBanTeamMate() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
        UUID teamMate = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(teamMate);
        Set<UUID> members = new HashSet<>();
        members.add(uuid);
        members.add(teamMate);
        when(im.getMembers(Mockito.any())).thenReturn(members);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.cannot-ban-member");
    }

    @Test
    public void testBanAlreadyBanned() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
        UUID bannedUser = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(bannedUser);
        when(island.isBanned(Mockito.eq(bannedUser))).thenReturn(true);
        assertFalse(ibc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.ban.player-already-banned");
    }

    @Test
    public void testBanOp() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
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
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
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
        Mockito.verify(targetUser).sendMessage("commands.island.ban.you-are-banned", "[owner]", user.getName());
    }

    @Test
    public void testBanOnlineUser() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
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
        Mockito.verify(targetUser).sendMessage("commands.island.ban.you-are-banned", "[owner]", user.getName());
    }
    
    @Test
    public void testCancelledBan() {
        IslandBanCommand ibc = new IslandBanCommand(ic);
        when(im.hasIsland(Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.eq(uuid))).thenReturn(true);
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
        Mockito.verify(targetUser, Mockito.never()).sendMessage("commands.island.ban.you-are-banned", "[owner]", user.getName());
    }
    
}
