package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;
import world.bentobox.bentobox.panels.customizable.IslandCreationPanel;

/**
 * @author tastybento
 *
 */
public class IslandCreateCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    private IslandCreateCommand cc;
    @Mock
    private Builder builder;
    @Mock
    private Settings settings;
    @Mock
    private CompositeCommand ic;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private @NonNull WorldSettings ws;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Player
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Return the default value for perm questions by default
        when(user.getPermissionValue(anyString(), anyInt()))
                .thenAnswer((Answer<Integer>) inv -> inv.getArgument(1, Integer.class));
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(mockPlayer);

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
        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        when(im.getPrimaryIsland(world, uuid)).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // IWM
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(ws.getConcurrentIslands()).thenReturn(1); // One island allowed
        when(iwm.getWorldSettings(world)).thenReturn(ws);
        when(iwm.getAddon(world)).thenReturn(Optional.of(addon));

        // NewIsland
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);
        when(builder.player(any())).thenReturn(builder);
        when(builder.name(Mockito.anyString())).thenReturn(builder);
        when(builder.addon(addon)).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));

        // Bundles manager

        @NonNull
        Map<String, BlueprintBundle> map = new HashMap<>();
        when(bpm.getBlueprintBundles(addon)).thenReturn(map);
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // IslandCreationPanel
        Mockito.mockStatic(IslandCreationPanel.class);

        // Command
        cc = new IslandCreateCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#IslandCreateCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testIslandCreateCommand() {
        assertEquals("create", cc.getLabel());
        assertEquals("new", cc.getAliases().getFirst());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#setup()}.
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
        verify(user).sendMessage("commands.island.create.you-cannot-make");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUserStringListOfStringZeroAllowed() {
        when(ws.getConcurrentIslands()).thenReturn(0); // No islands allowed
        assertFalse(cc.canExecute(user, "", Collections.emptyList()));
        verify(user).sendMessage("commands.island.create.you-cannot-make");

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
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
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
        verify(builder).player(user);
        verify(builder).addon(any());
        verify(builder).reason(Reason.CREATE);
        verify(builder).name("custom");
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
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringUnknownBundle() {
        assertFalse(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.unknown-blueprint");
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
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
        verify(builder).player(user);
        verify(builder).addon(any());
        verify(builder).reason(Reason.CREATE);
        verify(builder).name("custom");
        verify(builder).build();
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCooldown() {
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        verify(ic, never()).getSubCommand("reset");
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
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandCreateCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringShowPanel() {
        Map<String, BlueprintBundle> map = Map.of("bundle1", new BlueprintBundle(), "bundle2", new BlueprintBundle(),
                "bundle3", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        assertTrue(cc.execute(user, "", Collections.emptyList()));
        // Panel is shown, not the creation message
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for cost check - cannot afford
     */
    @Test
    public void testMakeIslandWithCostCannotAfford() {
        // Multiple bundles
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Economy enabled
        when(settings.isUseEconomy()).thenReturn(true);
        // Vault present but cannot afford
        VaultHook vault = mock(VaultHook.class);
        when(vault.has(any(User.class), eq(100.0))).thenReturn(false);
        when(vault.format(100.0)).thenReturn("$100.00");
        when(plugin.getVault()).thenReturn(Optional.of(vault));

        assertFalse(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.cannot-afford", "[cost]", "$100.00");
        verify(user, never()).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for cost check - can afford
     */
    @Test
    public void testMakeIslandWithCostCanAfford() {
        // Multiple bundles
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Economy enabled
        when(settings.isUseEconomy()).thenReturn(true);
        // Vault present and can afford
        VaultHook vault = mock(VaultHook.class);
        when(vault.has(any(User.class), eq(100.0))).thenReturn(true);
        when(vault.format(100.0)).thenReturn("$100.00");
        when(plugin.getVault()).thenReturn(Optional.of(vault));

        assertTrue(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        verify(vault).withdraw(user, 100.0);
    }

    /**
     * Test method for cost check - single bundle ignores cost
     */
    @Test
    public void testMakeIslandCostIgnoredSingleBundle() {
        // Single bundle with cost
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(settings.isUseEconomy()).thenReturn(true);

        assertTrue(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        // No vault interaction
        verify(plugin, never()).getVault();
    }

    /**
     * Test method for cost check - economy disabled ignores cost
     */
    @Test
    public void testMakeIslandCostIgnoredNoEconomy() {
        // Multiple bundles
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Economy disabled
        when(settings.isUseEconomy()).thenReturn(false);

        assertTrue(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for cost check - no vault ignores cost
     */
    @Test
    public void testMakeIslandCostIgnoredNoVault() {
        // Multiple bundles
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(settings.isUseEconomy()).thenReturn(true);
        // No vault
        when(plugin.getVault()).thenReturn(Optional.empty());

        assertTrue(cc.execute(user, "", List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
    }
}
