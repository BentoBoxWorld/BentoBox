package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class})
public class IslandNearCommandTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private @Nullable Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private Player pp;

    private UUID uuid;

    private IslandNearCommand inc;
    @Mock
    private @Nullable Location location;
    @Mock
    private Block block;

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
        // Player
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(uuid);
        User.getInstance(p);
        when(p.isOnline()).thenReturn(true);
        // User
        User.setPlugin(plugin);
        when(pm.getName(any())).thenReturn("tastybento");

        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isOnline()).thenReturn(true);
        when(user.getPlayer()).thenReturn(p);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // World
        when(ic.getWorld()).thenReturn(world);

        // IWM friendly name for help
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(any())).thenReturn(400);


        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getIslandAt(any(Location.class))).thenReturn(optionalIsland);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Island
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(island.getCenter()).thenReturn(location);
        when(island.getOwner()).thenReturn(uuid);
        when(location.getBlock()).thenReturn(block);
        when(block.getRelative(any(), anyInt())).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(island.getName()).thenReturn("Island name");


        // The command
        inc = new IslandNearCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("island.near", inc.getPermission());
        assertTrue(inc.isOnlyPlayer());
        assertEquals("commands.island.near.parameters", inc.getParameters());
        assertEquals("commands.island.near.description", inc.getDescription());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteWithArgsShowHelp() {
        assertFalse(inc.canExecute(user, "near", Collections.singletonList("fghjk")));
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq("BSkyBlock"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteHasTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any())).thenReturn(true);
        assertTrue(inc.canExecute(user, "near", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteHasIslandAndTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(true);
        assertTrue(inc.canExecute(user, "near", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteHasIslandNoTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertTrue(inc.canExecute(user, "near", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIslandNoTeam() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertFalse(inc.canExecute(user, "near", Collections.emptyList()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringAllFourPoints() {
        assertTrue(inc.execute(user, "near", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.near.the-following-islands"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.north"),
                eq(TextVariables.NAME), eq("Island name"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.east"),
                eq(TextVariables.NAME), eq("Island name"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.south"),
                eq(TextVariables.NAME), eq("Island name"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.west"),
                eq(TextVariables.NAME), eq("Island name"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnowned() {
        when(island.getName()).thenReturn("");
        when(island.isUnowned()).thenReturn(true);
        assertTrue(inc.execute(user, "near", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.near.the-following-islands"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.north"),
                eq(TextVariables.NAME), eq("commands.admin.info.unowned"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.east"),
                eq(TextVariables.NAME), eq("commands.admin.info.unowned"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.south"),
                eq(TextVariables.NAME), eq("commands.admin.info.unowned"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.west"),
                eq(TextVariables.NAME), eq("commands.admin.info.unowned"));
    }
    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoName() {
        when(island.getName()).thenReturn("");
        assertTrue(inc.execute(user, "near", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.near.the-following-islands"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.north"),
                eq(TextVariables.NAME), eq("tastybento"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.east"),
                eq(TextVariables.NAME), eq("tastybento"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.south"),
                eq(TextVariables.NAME), eq("tastybento"));
        verify(user).sendMessage(eq("commands.island.near.syntax"),
                eq("[direction]"), eq("commands.island.near.west"),
                eq(TextVariables.NAME), eq("tastybento"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandNearCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoIslands() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertTrue(inc.execute(user, "near", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.near.the-following-islands"));
        verify(user, never()).sendMessage(any(), any(), any(), any(), any());
        verify(user).sendMessage(eq("commands.island.near.no-neighbors"));
    }

}
