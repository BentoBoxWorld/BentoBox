/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandBanlistCommandTest {

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
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
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");

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

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

    }

    /**
     * Test method for .
     */
    @Test
    public void testWithArgs() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        assertFalse(iubc.execute(user, iubc.getLabel(), Collections.singletonList("bill")));
        // Verify show help
    }

    @Test
    public void testNoIsland() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        assertFalse(iubc.execute(user, iubc.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testBanlistNooneBanned() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        assertTrue(iubc.execute(user, iubc.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.banlist.noone");
    }

    @Test
    public void testBanlistBanned() {
        IslandBanlistCommand iubc = new IslandBanlistCommand(ic);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        // Make a ban list
        String[] names = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};
        Set<UUID> banned = new HashSet<>();
        Map<UUID, String> uuidToName = new HashMap<>();
        for (String name : names) {
            UUID uuid = UUID.randomUUID();
            banned.add(uuid);
            uuidToName.put(uuid, name);
        }
        when(island.getBanned()).thenReturn(banned);
        // Respond to name queries
        when(pm.getName(Mockito.any(UUID.class))).then((Answer<String>) invocation -> uuidToName.getOrDefault(invocation.getArgumentAt(0, UUID.class), "tastybento"));
        assertTrue(iubc.execute(user, iubc.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage("commands.island.banlist.the-following");
    }

}
