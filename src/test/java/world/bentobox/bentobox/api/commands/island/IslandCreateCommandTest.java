/**
 *
 */
package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;

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
    private BentoBox plugin;
    @Mock
    private Settings settings;
    @Mock
    private CompositeCommand ic;
    @Mock
    private BlueprintsManager bpm;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("deprecation")
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

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);


        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);


        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);


        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // NewIsland
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        when(builder.player(any())).thenReturn(builder);
        when(builder.name(Mockito.anyString())).thenReturn(builder);
        when(builder.world(any())).thenReturn(builder);
        when(builder.addon(addon)).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

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
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(true);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("general.errors.already-have-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringInTeam() {
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), Mockito.any(UUID.class))).thenReturn(true);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("general.errors.already-have-island"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringTooManyIslands() {
        when(im.hasIsland(any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), Mockito.any(UUID.class))).thenReturn(false);
        when(iwm.getMaxIslands(any())).thenReturn(100);
        when(im.getIslandCount(any())).thenReturn(100);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("commands.island.create.too-many-islands"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringSuccess() throws IOException {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);

        assertTrue(cc.execute(user, "", Collections.singletonList("custom")));
        verify(builder).player(eq(user));
        verify(builder).addon(any());
        verify(builder).reason(eq(Reason.CREATE));
        verify(builder).name(eq("custom"));
        verify(builder).build();
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringThrowException() throws IOException {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);

        when(builder.build()).thenThrow(new IOException("message"));
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        verify(user).sendMessage("commands.island.create.unable-create-island");
        verify(plugin).logError("Could not create island for player. message");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringBundleNoPermission() {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // No permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(false);
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownBundle() {
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        verify(user).sendMessage(eq("commands.island.create.unknown-blueprint"));
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoBundle() {
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        //verify(bpm).showPanel(any(), any(), any());
        //TODO verify it is calling the panel
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringKnownBundle() throws IOException {
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(bpm.validate(any(), any())).thenReturn("custom");
        assertTrue(cc.execute(user, "", Collections.singletonList("custom")));
        verify(builder).player(eq(user));
        verify(builder).addon(any());
        verify(builder).reason(eq(Reason.CREATE));
        verify(builder).name(eq("custom"));
        verify(builder).build();
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCooldown() {
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        verify(ic, never()).getSubCommand(eq("reset"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNoCooldown() {
        when(settings.isResetCooldownOnCreate()).thenReturn(true);
        assertTrue(cc.execute(user, "", Collections.emptyList()));
    }
}
