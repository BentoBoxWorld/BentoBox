package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

public class BannedCommandsTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFallingBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        
        // Player
        when(mockPlayer.isOp()).thenReturn(false);
        when(mockPlayer.hasPermission(Mockito.anyString())).thenReturn(false);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getLocation()).thenReturn(mock(Location.class));
        User.getInstance(mockPlayer);

        server.setPlayers(10);
        when(mockPlayer.getServer()).thenReturn(server);

        // Island manager
        // Default not on island, so is a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Set flag
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, true);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testInstantReturn() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);

        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);

        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // In world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        // Op
        when(mockPlayer.isOp()).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // Not op
        when(mockPlayer.isOp()).thenReturn(false);
        // Has bypass perm
        when(mockPlayer.hasPermission(Mockito.anyString())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

        // Does not have perm
        when(mockPlayer.hasPermission(Mockito.anyString())).thenReturn(false);
        // Not a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testEmptyBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
        verify(iwm).getVisitorBannedCommands(any());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommandWithExtraBannedStuff() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command with extra stuff");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand2() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/spawn");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand3() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/cmi sethome");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedComman4() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/cmi");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand5() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/cmi homey");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand6() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/spawn");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        banned.add("spawn sethome now");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedCommand7() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/spawn");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("cmi sethome");
        banned.add("spawn sethome now");
        banned.add("cmi multi now");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());

    }


    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithNothing() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testAnotherBannedCommandsWithBannedCommandWithExtra() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/another_banned_command with extra stuff");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(banned);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());

    }


    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommand() {
        when(mockPlayer.getFallDistance()).thenReturn(10F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNotFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNoFlag() {
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, false);
        when(mockPlayer.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

}
