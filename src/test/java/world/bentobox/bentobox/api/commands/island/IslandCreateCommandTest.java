package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
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
import world.bentobox.bentobox.api.configuration.WorldSettings;
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
    @Mock
    private World world;
    @Mock
    private @NonNull WorldSettings ws;
    @Mock
    private Island island;

    /**
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
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Return the default value for perm questions by default
        when(user.getPermissionValue(anyString(), anyInt())).thenAnswer((Answer<Integer>) inv -> inv.getArgument(1, Integer.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);
        when(addon.getPermissionPrefix()).thenReturn("bskyblock.");

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);
        when(ic.getWorld()).thenReturn(world);


        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);


        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // IWM
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(ws.getConcurrentIslands()).thenReturn(1); // One island allowed
        when(iwm.getWorldSettings(world)).thenReturn(ws);
        when(iwm.getAddon(world)).thenReturn(Optional.of(addon));
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
        // Currently user has two islands
        when(im.getNumberOfConcurrentIslands(user.getUniqueId(), world)).thenReturn(2);
        // Player has an island
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.you-cannot-make");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringZeroAllowed() {
        when(ws.getConcurrentIslands()).thenReturn(0); // No islands allowed
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("general.errors.you-cannot-make");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringHasPerm() {
        // Currently user has two islands
        when(im.getNumberOfConcurrentIslands(user.getUniqueId(), world)).thenReturn(19);
        // Perm
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(20); // 20 allowed!
        assertTrue(cc.canExecute(user, "", Collections.emptyList()));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringHasIslandReserved() {
        @Nullable
        Island island = mock(Island.class);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(island.isReserved()).thenReturn(true);
        assertTrue(cc.canExecute(user, "", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.already-have-island");

    }



    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringTooManyIslands() {
        when(im.getPrimaryIsland(any(), any(UUID.class))).thenReturn(null);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        when(iwm.getMaxIslands(any())).thenReturn(100);
        when(im.getIslandCount(any())).thenReturn(100L);
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.island.create.too-many-islands");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringSuccess() throws Exception {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);

        assertTrue(cc.execute(user, "", List.of("custom")));
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
    public void testExecuteUserStringListOfStringThrowException() throws Exception {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);

        when(builder.build()).thenThrow(new IOException("commands.island.create.unable-create-island"));
        assertFalse(cc.execute(user, "", List.of("custom")));
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
        assertFalse(cc.execute(user, "", List.of("custom")));
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
        Map<String, BlueprintBundle> map = Map.of("bundle1", new BlueprintBundle(),
                "bundle2", new BlueprintBundle(),
                "bundle3", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        // Panel is shown, not the creation message
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }
}
