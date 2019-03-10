/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
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
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.SchemsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;
import world.bentobox.bentobox.schems.Clipboard;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, NewIsland.class})
public class IslandCreateCommandTest {

    @Mock
    private User user;
    private IslandCreateCommand cc;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Builder builder;
    @Mock
    private SchemsManager sm;
    @Mock
    private BentoBox plugin;
    @Mock
    private Settings settings;
    @Mock
    private CompositeCommand ic;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer((Answer<String>) invocation -> invocation.getArgumentAt(0, String.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());


        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);


        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // IWM friendly name
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // NewIsland
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        when(builder.player(Mockito.any())).thenReturn(builder);
        when(builder.name(Mockito.anyString())).thenReturn(builder);
        when(builder.world(Mockito.any())).thenReturn(builder);
        when(builder.reason(Mockito.any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));


        // Schems manager
        Map<String, Clipboard> map = new HashMap<>();
        when(sm.get(Mockito.any())).thenReturn(map);
        when(plugin.getSchemsManager()).thenReturn(sm);

        // Command
        cc = new IslandCreateCommand(ic);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#IslandCreateCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandCreateCommand() {
        assertEquals("create", cc.getLabel());
        assertEquals("new", cc.getAliases().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertTrue(cc.isOnlyPlayer());
        assertEquals("commands.island.create.parameters", cc.getParameters());
        assertEquals("commands.island.create.description", cc.getDescription());
        assertEquals("permission.island.create", cc.getPermission());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringHasIsland() {
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.already-have-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringInTeam() {
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.already-have-island"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringTooManyIslands() {
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(iwm.getMaxIslands(Mockito.any())).thenReturn(100);
        when(im.getIslandCount(Mockito.any())).thenReturn(100);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.create.too-many-islands"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringSuccess() throws IOException {
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        Mockito.verify(builder).player(Mockito.eq(user));
        Mockito.verify(builder).world(Mockito.any());
        Mockito.verify(builder).reason(Mockito.eq(Reason.CREATE));
        Mockito.verify(builder).name(Mockito.eq("island"));
        Mockito.verify(builder).build();
        Mockito.verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringThrowException() throws IOException {
        when(builder.build()).thenThrow(new IOException("message"));
        assertFalse(cc.execute(user, "", Collections.emptyList()));
        Mockito.verify(user).sendMessage("commands.island.create.creating-island");
        Mockito.verify(user).sendMessage("commands.island.create.unable-create-island");
        Mockito.verify(plugin).logError("Could not create island for player. message");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSchemNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-permission"), Mockito.eq(TextVariables.PERMISSION), Mockito.eq("permission.island.create.custom"));
        Mockito.verify(user, Mockito.never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownSchem() {
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.create.unknown-schem"));
        Mockito.verify(user, Mockito.never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringKnownSchem() throws IOException {
        when(sm.get(Mockito.any())).thenReturn(Collections.singletonMap("custom", mock(Clipboard.class)));
        assertTrue(cc.execute(user, "", Collections.singletonList("custom")));
        Mockito.verify(builder).player(Mockito.eq(user));
        Mockito.verify(builder).world(Mockito.any());
        Mockito.verify(builder).reason(Mockito.eq(Reason.CREATE));
        Mockito.verify(builder).name(Mockito.eq("custom"));
        Mockito.verify(builder).build();
        Mockito.verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCooldown() {
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        Mockito.verify(ic, Mockito.never()).getSubCommand(Mockito.eq("reset"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoCooldown() {
        when(settings.isResetCooldownOnCreate()).thenReturn(true);
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        Mockito.verify(ic).getSubCommand(Mockito.eq("reset"));
    }
}
