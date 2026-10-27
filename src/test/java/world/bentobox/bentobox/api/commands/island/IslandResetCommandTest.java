package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * @author tastybento
 *
 */
class IslandResetCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private PlayersManager pm;
    @Mock
    private BlueprintsManager bpm;

    private IslandResetCommand irc;

    @Test
    void testFoliaResetLeavesOldIslandCleanupToNewIsland() {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        when(builder.buildAsync()).thenReturn(new java.util.concurrent.CompletableFuture<>());
        try (var creation = Mockito.mockStatic(NewIsland.class);
                var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            BlueprintCostHelperTest.immediate(tasks);
            creation.when(NewIsland::builder).thenReturn(builder);
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
            // Member cleanup moved to NewIsland.tidyUp: a failed teleport must leave the old
            // island's members untouched instead of producing a ghost island.
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
            var callback = org.mockito.ArgumentCaptor.forClass(java.util.function.Supplier.class);
            verify(builder).beforeTeleport(callback.capture());
            assertTrue(((java.util.concurrent.CompletableFuture<?>) callback.getValue().get()).isDone());
            verify(pm, never()).addReset(any(), any());
        }
    }

    @Test
    void testPreclearCancellationDoesNotRemoveMembersOrBuildIsland() throws Exception {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        org.mockito.Mockito.doAnswer(call -> {
            var event = call.getArgument(0);
            if (event instanceof world.bentobox.bentobox.api.events.island.IslandPreclearEvent preclear) {
                preclear.setCancelled(true);
            }
            return null;
        }).when(pim).callEvent(any(org.bukkit.event.Event.class));
        try (var creation = Mockito.mockStatic(NewIsland.class)) {
            assertFalse(irc.execute(user, irc.getLabel(), List.of("custom")));
            creation.verify(NewIsland::builder, never());
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
            verify(pm, never()).addReset(any(), any());
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void testCancelledOrFailedBuildDoesNotConsumeResetOrCharge(boolean failure) throws Exception {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(s.isChargeForBlueprintOnReset()).thenReturn(true);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        if (failure) when(builder.build()).thenThrow(new IOException("creation failed"));
        else when(builder.build()).thenReturn(null);
        try (var creation = Mockito.mockStatic(NewIsland.class);
                var costs = Mockito.mockStatic(BlueprintCostHelper.class)) {
            creation.when(NewIsland::builder).thenReturn(builder);
            assertFalse(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(pm, never()).addReset(any(), any());
            verify(builder, never()).build();
            verify(im, never()).deleteIsland(eq(island), anyBoolean(), any());
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
        }
    }

    @Test
    void testAttemptThatNeverStartedDoesNotChargeTheResetCooldown() {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(s.getResetCooldown()).thenReturn(300);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(pm.getResetsLeft(world, uuid)).thenReturn(-1);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        try (var creation = Mockito.mockStatic(NewIsland.class);
                var costs = Mockito.mockStatic(BlueprintCostHelper.class)) {
            creation.when(NewIsland::builder).thenReturn(builder);
            costs.when(() -> BlueprintCostHelper.build(any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                    any(), any())).thenReturn(false);
            assertFalse(irc.execute(user, irc.getLabel(), List.of("custom")));
            // Nothing was created (no money, another creation running, ...), so the cooldown
            // must not be charged for it.
            assertTrue(irc.canExecute(user, irc.getLabel(), List.of("custom")));
            verify(user, never()).sendMessage(eq("general.errors.you-must-wait"), eq(TextVariables.NUMBER), anyString());
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void testAsyncCancelledOrFailedBuildDoesNotConsumeReset(boolean failure) {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        var pending = new java.util.concurrent.CompletableFuture<Island>();
        when(builder.buildAsync()).thenReturn(pending);
        try (var creation = Mockito.mockStatic(NewIsland.class);
                var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            BlueprintCostHelperTest.immediate(tasks);
            creation.when(NewIsland::builder).thenReturn(builder);
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.global(any(), any())).thenAnswer(call -> {
                call.getArgument(1, Runnable.class).run();
                return mock(org.bukkit.scheduler.BukkitTask.class);
            });
            assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(pm, never()).addReset(any(), any());
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
            if (failure) pending.completeExceptionally(new IOException("creation failed"));
            else pending.complete(null);
            verify(pm, never()).addReset(any(), any());
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
            tasks.verify(() -> TaskScheduler.user(eq(plugin), eq(user), any()), failure ? Mockito.times(1) : never());
            if (failure) verify(plugin).logError(Mockito.contains("creation failed"));
            else verify(plugin, never()).logError(anyString());
        }
    }

    @Test
    void testAsyncSuccessCountsResetEvenIfPlayerTaskCannotRun() {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        var pending = new java.util.concurrent.CompletableFuture<Island>();
        when(builder.buildAsync()).thenReturn(pending);
        try (var creation = Mockito.mockStatic(NewIsland.class);
                var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            BlueprintCostHelperTest.immediate(tasks);
            creation.when(NewIsland::builder).thenReturn(builder);
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.globalFuture(any(), any())).thenAnswer(call -> {
                call.getArgument(1, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(pm, never()).addReset(any(), any());
            var settlement = org.mockito.ArgumentCaptor.forClass(java.util.function.Function.class);
            verify(builder).afterBuild(settlement.capture());
            ((java.util.concurrent.CompletableFuture<?>) settlement.getValue().apply(island)).join();
            pending.complete(island);
            verify(pm).addReset(world, uuid);
            tasks.verify(() -> TaskScheduler.globalFuture(eq(plugin), any()), Mockito.atLeastOnce());
        }
    }

    @Test
    void testPendingCreationRejectsDelayedConfirmationBeforeCleanup() {
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        try (var creation = Mockito.mockStatic(NewIsland.class)) {
            creation.when(() -> NewIsland.isCreating(user)).thenReturn(true);
            assertFalse(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(im, never()).removePlayer(any(Island.class), any(UUID.class));
            verify(pm, never()).addReset(any(), any());
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getDataFolder()).thenReturn(java.nio.file.Files.createTempDirectory("blueprint-reset-test").toFile());
        Mockito.mockStatic(IslandsManager.class, Mockito.RETURNS_MOCKS);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.spigot()).thenReturn(spigot);
        when(p.isOnline()).thenReturn(true);
        User.getInstance(p);

        // User
        User.setPlugin(plugin);

        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isOnline()).thenReturn(true);
        when(user.getPlayer()).thenReturn(p);
        when(user.getTranslation(any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // World
        when(ic.getWorld()).thenReturn(world);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        mockedBukkit.when(Bukkit::getScheduler).thenReturn(sch);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        when(bpm.validate(any(), any())).thenReturn("custom");

        // Give the user some resets
        when(pm.getResetsLeft(world, uuid)).thenReturn(3);

        // Island team members
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        // Put a team on the island
        for (int j = 0; j < 11; j++) {
            UUID temp = UUID.randomUUID();
            when(mockPlayer.getUniqueId()).thenReturn(temp);
            User.getInstance(mockPlayer);
            members.add(temp);
        }
        when(island.getMemberSet()).thenReturn(members.build());
        when(location.clone()).thenReturn(location);
        when(island.getCenter()).thenReturn(location);
        when(island.getHistory()).thenReturn(Collections.emptyList());
        when(island.getSpawnPoint()).thenReturn(Collections.emptyMap());

        // Addon
        GameModeAddon addon1 = mock(GameModeAddon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "BSkyBlock", "1.0.0").build();
        when(addon1.getDescription()).thenReturn(desc);
        when(ic.getAddon()).thenReturn(addon1);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // The command
        irc = new IslandResetCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }
    /**
     * Test method for
     * {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    void testNoIsland() {
        // Test the reset command
        // Does not have island
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.not-owner");
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    void testNoResetsLeft() {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        // Block based on no resets left
        when(pm.getResetsLeft(world, uuid)).thenReturn(0);

        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.reset.none-left");
        // Verify event
        verify(pim, never()).callEvent(any(IslandBaseEvent.class));
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testNoConfirmationRequired() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // TODO Verify that panel was shown
        // Verify event (13 * 2)
        verify(pim, times(2)).callEvent(any(IslandBaseEvent.class));
        // Verify messaging
        verify(user).sendMessage("commands.island.create.creating-island");
        verify(user, never()).sendMessage(eq("commands.island.reset.kicked-from-island"), eq(TextVariables.GAMEMODE), anyString());
        // Member cleanup is deferred until the replacement island is complete.
        checkSpigotMessage("commands.island.reset.kicked-from-island", 0);
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    void testUnlimitedResets() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);
        // Test with unlimited resets
        when(pm.getResetsLeft(world, uuid)).thenReturn(-1);

        // Reset
        assertTrue(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for
     * {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    void testNoPaste() throws Exception {
        irc = new IslandResetCommand(ic, true);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);
        // Test with unlimited resets
        when(pm.getResetsLeft(world, uuid)).thenReturn(-1);

        // Reset
        assertTrue(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(builder, never()).noPaste();
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testConfirmationRequired() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(world, uuid)).thenReturn(1);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        // Require confirmation
        when(s.isResetConfirmation()).thenReturn(true);
        when(s.getConfirmationTime()).thenReturn(20);

        // Reset
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // Check for message
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", String.valueOf(s.getConfirmationTime()));

        // Send command again to confirm
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // Some more checking can go here...
    }

    /**
     * When dialog confirmations are enabled but the dialog cannot be shown (no live
     * dialog registry, as under test), the command must fall back to the classic
     * type-again prompt rather than silently losing the confirmation.
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testConfirmationDialogFallsBackWhenUnavailable() throws Exception {
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        when(pm.getResetsLeft(world, uuid)).thenReturn(1);
        when(im.getIsland(any(), eq(uuid))).thenReturn(mock(Island.class));

        // Require confirmation, and prefer a dialog for an online player
        when(s.isResetConfirmation()).thenReturn(true);
        when(s.getConfirmationTime()).thenReturn(20);
        when(s.isDialogConfirmations()).thenReturn(true);
        when(user.isPlayer()).thenReturn(true);

        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // The dialog could not be built under test, so we fell back to the prompt
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", String.valueOf(s.getConfirmationTime()));
        // and the failure was logged
        verify(plugin).logError(Mockito.contains("confirmation dialog"));
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testNoConfirmationRequiredUnknownBlueprint() {
        // No such bundle
        when(bpm.validate(any(), any())).thenReturn(null);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage(
                "commands.island.create.unknown-blueprint"
                );
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testNoConfirmationRequiredBlueprintNoPerm() {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // No permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(false);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    void testNoConfirmationRequiredCustomSchemHasPermission() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(world, uuid)).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        // Verify event (13 * 2)
        verify(pim, times(2)).callEvent(any(IslandBaseEvent.class));

    }

    /**
     * Test method for reset with cost - cannot afford
     */
    @Test
    void testResetIslandWithCostCannotAfford() {
        // Enable reset charging
        when(s.isChargeForBlueprintOnReset()).thenReturn(true);
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
        when(s.isUseEconomy()).thenReturn(true);
        // Vault present but cannot afford
        VaultHook vault = mock(VaultHook.class);
        when(vault.has(any(User.class), eq(100.0))).thenReturn(false);
        when(vault.format(100.0)).thenReturn("$100.00");
        when(plugin.getVault()).thenReturn(Optional.of(vault));

        var debit = mock(VaultHook.CapturedWithdrawal.class);
        when(debit.providerIdentity()).thenReturn("provider");
        when(debit.get()).thenReturn(new net.milkbowl.vault.economy.EconomyResponse(0, 0,
                net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "insufficient balance"));
        when(vault.captureRefundableWithdrawal(user, 100, world)).thenReturn(debit);
        NewIsland.Builder builder = mock(NewIsland.Builder.class, Mockito.RETURNS_SELF);
        try (var creation = Mockito.mockStatic(NewIsland.class); var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            creation.when(NewIsland::builder).thenReturn(builder);
            BlueprintCostHelperTest.immediate(tasks);
            assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(builder, never()).buildAsync();
        }
    }

    /**
     * Test method for reset with cost - can afford
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void testResetIslandWithCostCanAfford(boolean paymentSucceeds) throws Exception {
        // Enable reset charging
        when(s.isChargeForBlueprintOnReset()).thenReturn(true);
        // Now has island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
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
        when(s.isUseEconomy()).thenReturn(true);
        // Vault present and can afford
        VaultHook vault = mock(VaultHook.class);
        when(vault.has(any(User.class), eq(100.0))).thenReturn(true);
        when(vault.withdraw(user, 100.0)).thenReturn(new net.milkbowl.vault.economy.EconomyResponse(
                100, 0, paymentSucceeds ? net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS
                        : net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "payment rejected"));
        when(vault.format(100.0)).thenReturn("$100.00");
        when(plugin.getVault()).thenReturn(Optional.of(vault));

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        var debit = mock(VaultHook.CapturedWithdrawal.class);
        when(debit.providerIdentity()).thenReturn("provider");
        when(debit.get()).thenReturn(new net.milkbowl.vault.economy.EconomyResponse(100, 0,
                paymentSucceeds ? net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS
                        : net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "result"));
        when(vault.captureRefundableWithdrawal(user, 100, world)).thenReturn(debit);
        when(builder.transactional()).thenReturn(builder);
        when(builder.allocated(any())).thenReturn(builder);
        when(builder.beforeTeleport(any())).thenReturn(builder);
        when(builder.afterBuild(any())).thenReturn(builder);
        when(builder.buildAsync()).thenReturn(new java.util.concurrent.CompletableFuture<>());
        try (var tasks = Mockito.mockStatic(TaskScheduler.class)) {
            BlueprintCostHelperTest.immediate(tasks);
            assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
            verify(debit).get();
            verify(builder, paymentSucceeds ? times(1) : never()).buildAsync();
        }
    }

    /**
     * Test method for reset cost - disabled by config (default)
     */
    @Test
    void testResetIslandCostDisabledByConfig() throws Exception {
        // Reset charging disabled (default)
        when(s.isChargeForBlueprintOnReset()).thenReturn(false);
        // Now has island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        // Multiple bundles with cost
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(s.isUseEconomy()).thenReturn(true);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        assertTrue(irc.execute(user, irc.getLabel(), List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        // No vault interaction since charging is disabled
        verify(plugin, never()).getVault();
    }

    /**
     * Test method for reset cost - no vault ignores cost
     */
    @Test
    void testResetIslandCostIgnoredNoVault() throws Exception {
        // Enable reset charging
        when(s.isChargeForBlueprintOnReset()).thenReturn(true);
        // Now has island
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);
        // Multiple bundles with cost
        BlueprintBundle bb = new BlueprintBundle();
        bb.setCost(100.0);
        Map<String, BlueprintBundle> map = new HashMap<>();
        map.put("custom", bb);
        map.put("default", new BlueprintBundle());
        when(bpm.getBlueprintBundles(any())).thenReturn(map);
        when(bpm.validate(any(), any())).thenReturn("custom");
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        when(s.isUseEconomy()).thenReturn(true);
        // No vault
        when(plugin.getVault()).thenReturn(Optional.empty());

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(NewIsland::builder).thenReturn(builder);

        assertFalse(irc.execute(user, irc.getLabel(), List.of("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
    }
}
