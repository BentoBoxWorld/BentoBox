package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
public class IslandHomesCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    private UUID uuid;
 
@Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), any(User.class))).thenReturn(false);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.onIsland(any())).thenReturn(true);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        @NotNull
        Map<String, Location> homeMap = new HashMap<>();
        homeMap.put("Home", null);
        homeMap.put("Home2", null);
        homeMap.put("Home3", null);
        homeMap.put("Home4", null);
        when(island.getHomes()).thenReturn(homeMap);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(any())).thenReturn(3);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandHomesCommand#IslandHomesCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandHomesCommand() {
        IslandHomesCommand cmd = new IslandHomesCommand(ic);
        assertEquals("homes", cmd.getName());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandHomesCommand#setup()}.
     */
    @Test
    public void testSetup() {
        IslandHomesCommand isc = new IslandHomesCommand(ic);
        assertEquals("bskyblock.island.homes", isc.getPermission());
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.homes.description", isc.getDescription());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        // Player doesn't have an island
        IslandHomesCommand isc = new IslandHomesCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandHomesCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecute() {
        when(im.getIslands(world, user)).thenReturn(List.of(island));
        IslandHomesCommand isc = new IslandHomesCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandHomesCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        IslandHomesCommand isc = new IslandHomesCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
    }

}
