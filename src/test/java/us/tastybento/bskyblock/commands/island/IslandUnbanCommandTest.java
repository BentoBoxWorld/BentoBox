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
import java.util.Optional;
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
public class IslandUnbanCommandTest {

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
     * Test method for {@link us.tastybento.bskyblock.commands.island.IslandUnbanCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)}.
     */
    // Island ban command by itself

    // *** Error conditions ***
    // Unban without an island
    // Unban as not a team leader
    // Unban unknown user
    // Unban self
    // Unban someone not banned

    // *** Working conditions ***
    // Unban user

    @Test
    public void testNoArgs() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        assertFalse(iubc.execute(user, new ArrayList<>()));
    }

    @Test
    public void testNoIsland() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testNotOwner() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.not-leader");
    }

    @Test
    public void testUnknownUser() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(null);
        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.errors.unknown-player");
    }

    @Test
    public void testBanSelf() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.unban.cannot-unban-yourself");
    }

    @Test
    public void testBanNotBanned() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID bannedUser = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(bannedUser);
        when(island.isBanned(Mockito.eq(bannedUser))).thenReturn(false);
        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("commands.island.unban.player-not-banned");
    }

    @Test
    public void testUnbanUser() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID targetUUID = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUUID);        
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(targetUser);
        // Mark as banned
        when(island.isBanned(Mockito.eq(targetUUID))).thenReturn(true);

        // Allow removing from ban list
        when(island.removeFromBanList(Mockito.any())).thenReturn(true);

        assertTrue(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user).sendMessage("general.success");
        Mockito.verify(targetUser).sendMessage("commands.island.unban.you-are-unbanned", "[owner]", user.getName());
    }

    @Test
    public void testCancelledUnban() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        UUID targetUUID = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUUID);        
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(targetUser);
        // Mark as banned
        when(island.isBanned(Mockito.eq(targetUUID))).thenReturn(true);

        // Allow removing from ban list
        when(island.removeFromBanList(Mockito.any())).thenReturn(false);

        assertFalse(iubc.execute(user, Arrays.asList("bill")));
        Mockito.verify(user, Mockito.never()).sendMessage("general.success");
        Mockito.verify(targetUser, Mockito.never()).sendMessage("commands.island.unban.you-are-unbanned", "[owner]", user.getName());
    }

    @Test
    public void testTabComplete() {
        Set<UUID> banned = new HashSet<>();
        // Add ten people to the banned list
        for (int i = 0; i < 10; i++) {
            banned.add(UUID.randomUUID());
        }
        when(island.getBanned()).thenReturn(banned);
        when(pm.getName(Mockito.any())).thenReturn("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Optional<List<String>> result = iubc.tabComplete(user, "", new LinkedList<>());
        assertTrue(result.isPresent());
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
        assertTrue(Arrays.equals(names, result.get().toArray()));
    }
}
