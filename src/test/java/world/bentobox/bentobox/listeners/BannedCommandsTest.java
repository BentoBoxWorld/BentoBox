package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class BannedCommandsTest {

    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Player player;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandsManager im;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFallingBannedCommands(any())).thenReturn(new ArrayList<>());
        when(plugin.getIWM()).thenReturn(iwm);

        // Player
        when(player.isOp()).thenReturn(false);
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        when(player.getWorld()).thenReturn(mock(World.class));
        when(player.getLocation()).thenReturn(mock(Location.class));
        User.getInstance(player);
        Server server = mock(Server.class);
        Set<Player> onlinePlayers = new HashSet<>();
        for (int j = 0; j < 10; j++) {
            Player p = mock(Player.class);
            UUID uuid = UUID.randomUUID();
            when(p.getUniqueId()).thenReturn(uuid);
            when(p.getName()).thenReturn(uuid.toString());
            onlinePlayers.add(p);
        }
        when(server.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);
        when(player.getServer()).thenReturn(server);

        // Island manager
        // Default not on island, so is a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Notifier
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());


    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testInstantReturn() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);

        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);

        bvc.onCommand(e);
        assertFalse(e.isCancelled());

        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        // Op
        when(player.isOp()).thenReturn(true);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());

        // Not op
        when(player.isOp()).thenReturn(false);
        // Has bypass perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());

        // Does not have perm
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
        // Not a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testEmptyBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/blah with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testAnotherBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/another_banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }
    
    
    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommand() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(player, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

}
