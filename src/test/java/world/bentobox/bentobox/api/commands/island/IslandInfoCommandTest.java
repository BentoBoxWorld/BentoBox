package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class IslandInfoCommandTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;

    private Island island;

    private IslandInfoCommand iic;

    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private PlaceholdersManager phm;
    @Mock
    private @NonNull Location location;
    @Mock
    private IslandWorldManager iwm;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(plugin.getRanksManager()).thenReturn(new RanksManager());

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(player.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(player);
        when(user.isPlayer()).thenReturn(true);
        //user = User.getInstance(player);
        // Set the User class plugin as this one
        User.setPlugin(plugin);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the same string
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        // Translate
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Island manager
        island = new Island(location, uuid, 100);
        island.setRange(400);
        when(location.toVector()).thenReturn(new Vector(1,2,3));
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(optionalIsland);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUUID(any())).thenReturn(uuid);


        // Command
        iic = new IslandInfoCommand(ic);
    }

    /**
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("island.info", iic.getPermission());
        assertFalse(iic.isOnlyPlayer());
        assertEquals("commands.island.info.parameters", iic.getParameters());
        assertEquals("commands.island.info.description", iic.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringTooManyArgs() {
        assertFalse(iic.execute(user, "", Arrays.asList("hdhh", "hdhdhd")));
        verify(user).sendMessage("commands.help.header", "[label]", "commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsConsole() {
        CommandSender console = mock(CommandSender.class);
        User sender = User.getInstance(console);
        assertFalse(iic.execute(sender, "", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.help.header", "[label]", "commands.help.console");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(iic.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.info.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsSuccess() {
        assertTrue(iic.execute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.admin.info.title");
        verify(user).sendMessage(eq("commands.admin.info.owner"), eq("[owner]"), eq(null), eq("[uuid]"), anyString());
        verify(user).sendMessage("commands.admin.info.deaths", "[number]", "0");
        verify(user).sendMessage("commands.admin.info.resets-left", "[number]", "0", "[total]", "0");
        verify(user).sendMessage("commands.admin.info.team-members-title");
        verify(user).sendMessage("commands.admin.info.team-owner-format", "[name]", null, "[rank]", "ranks.owner");
        verify(user).sendMessage("commands.admin.info.island-center", "[xyz]", "0,0,0");
        verify(user).sendMessage("commands.admin.info.protection-range", "[range]", "100");
        verify(user).sendMessage("commands.admin.info.protection-coords", "[xz1]", "-100,0,-100", "[xz2]", "99,0,99");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsSuccess() {
        assertTrue(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("commands.admin.info.title");
        verify(user).sendMessage(eq("commands.admin.info.owner"), eq("[owner]"), eq(null), eq("[uuid]"), anyString());
        verify(user).sendMessage("commands.admin.info.deaths", "[number]", "0");
        verify(user).sendMessage("commands.admin.info.resets-left", "[number]", "0", "[total]", "0");
        verify(user).sendMessage("commands.admin.info.team-members-title");
        verify(user).sendMessage("commands.admin.info.team-owner-format", "[name]", null, "[rank]", "ranks.owner");
        verify(user).sendMessage("commands.admin.info.island-center", "[xyz]", "0,0,0");
        verify(user).sendMessage("commands.admin.info.protection-range", "[range]", "100");
        verify(user).sendMessage("commands.admin.info.protection-coords", "[xz1]", "-100,0,-100", "[xz2]", "99,0,99");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");

    }

}
