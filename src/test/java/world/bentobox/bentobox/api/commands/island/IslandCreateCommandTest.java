package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.eclipse.jdt.annotation.Nullable;
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
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;
import world.bentobox.bentobox.panels.IslandCreationPanel;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, NewIsland.class, IslandCreationPanel.class})
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
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
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
        when(builder.addon(addon)).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // IslandCreationPanel
        PowerMockito.mockStatic(IslandCreationPanel.class);

        // Command
        cc = new IslandCreateCommand(ic);
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
        @Nullable
        Island island = mock(Island.class);
        when(im.getIsland(any(), Mockito.any(User.class))).thenReturn(island);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage(eq("general.errors.already-have-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringHasIslandReserved() {
        @Nullable
        Island island = mock(Island.class);
        when(im.getIsland(any(), Mockito.any(User.class))).thenReturn(island);
        when(island.isReserved()).thenReturn(true);
        assertTrue(cc.canExecute(user, "", Collections.emptyList()));
        verify(user, never()).sendMessage(eq("general.errors.already-have-island"));

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
    public void testExecuteUserStringListOfStringSuccess() throws Exception {
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
    public void testExecuteUserStringListOfStringThrowException() throws Exception {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);

        when(builder.build()).thenThrow(new IOException("commands.island.create.unable-create-island"));
        assertFalse(cc.execute(user, "", Collections.singletonList("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        verify(user).sendMessage("commands.island.create.unable-create-island");
        verify(plugin).logError("Could not create island for player. commands.island.create.unable-create-island");
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
    public void testExecuteUserStringListOfStringNoBundleNoPanel() {
        // Creates default bundle
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        // do not show panel, just make the island
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     * @throws IOException
     */
    @Test
    public void testExecuteUserStringListOfStringKnownBundle() throws Exception {
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

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringShowPanel() {
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("bundle1", new BlueprintBundle());
        map.put("bundle2", new BlueprintBundle());
        map.put("bundle3", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        // Panel is shown, not the creation message
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }
}
