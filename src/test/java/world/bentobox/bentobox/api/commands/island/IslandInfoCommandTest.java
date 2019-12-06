package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
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
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;

    @Mock
    private Island island;

    private IslandInfoCommand iic;

    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private PlaceholdersManager phm;

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
        when(player.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("tastybento");
        when(player.getWorld()).thenReturn(world);
        user = User.getInstance(player);
        // Set the User class plugin as this one
        User.setPlugin(plugin);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the same string
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // Island manager
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getIslandAt(any())).thenReturn(optionalIsland);
        when(island.showInfo(any())).thenReturn(true);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // Players manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUUID(any())).thenReturn(uuid);


        // Command
        iic = new IslandInfoCommand(ic);
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
        verify(player).sendMessage("commands.help.header");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsConsole() {
        CommandSender console = mock(CommandSender.class);
        User sender = User.getInstance(console);
        assertFalse(iic.execute(sender, "", Collections.emptyList()));
        verify(console).sendMessage("commands.help.header");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsNoIslandFalseInfo() {
        when(island.showInfo(any())).thenReturn(false);
        assertFalse(iic.execute(user, "", Collections.emptyList()));
        verify(player).sendMessage("commands.admin.info.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertFalse(iic.execute(user, "", Collections.emptyList()));
        verify(player).sendMessage("commands.admin.info.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoArgsSuccess() {
        assertTrue(iic.execute(user, "", Collections.emptyList()));
        verify(island).showInfo(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsSuccess() {
        assertTrue(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(island).showInfo(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(player).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandInfoCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringArgsUnknownPlayer() {
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(iic.execute(user, "", Collections.singletonList("tastybento")));
        verify(player).sendMessage("general.errors.unknown-player");
        verify(phm).replacePlaceholders(player, "general.errors.unknown-player");
    }

}
