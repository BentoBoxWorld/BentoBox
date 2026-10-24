package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.util.TaskScheduler;

class BlueprintCostHelperTest {
    @TempDir Path directory;
    private final BentoBox plugin = mock(BentoBox.class);
    private final GameModeAddon addon = mock(GameModeAddon.class);
    private final User user = mock(User.class);
    private final World world = mock(World.class);
    private final VaultHook vault = mock(VaultHook.class);
    private final VaultHook.CapturedWithdrawal withdrawal = mock(VaultHook.CapturedWithdrawal.class);
    private final NewIsland.Builder builder = mock(NewIsland.Builder.class, RETURNS_SELF);

    @BeforeEach void setUp() {
        var blueprints = mock(BlueprintsManager.class);
        var settings = mock(Settings.class);
        var bundle = new BlueprintBundle();
        bundle.setCost(100);
        when(plugin.getBlueprintsManager()).thenReturn(blueprints);
        when(blueprints.getBlueprintBundles(addon)).thenReturn(Map.of("paid", bundle, "default", new BlueprintBundle()));
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.isUseEconomy()).thenReturn(true);
        when(plugin.getVault()).thenReturn(Optional.of(vault));
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("islands");
        when(vault.captureRefundableWithdrawal(user, 100, world)).thenReturn(withdrawal);
        when(withdrawal.providerIdentity()).thenReturn("test-provider");
        when(withdrawal.get()).thenReturn(new EconomyResponse(100, 0, ResponseType.SUCCESS, null));
        when(withdrawal.refund()).thenReturn(new EconomyResponse(100, 100, ResponseType.SUCCESS, null));
    }

    private boolean build() {
        return BlueprintCostHelper.build(plugin, addon, user, "paid", world, builder, true,
                () -> CompletableFuture.completedFuture(null), () -> {});
    }

    @Test void withdrawalCompletesBeforeBuildingAndCancellationRefunds() {
        when(builder.buildAsync()).thenReturn(CompletableFuture.completedFuture(null));
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(build());
            var order = inOrder(withdrawal, builder);
            order.verify(withdrawal).get();
            order.verify(builder).buildAsync();
            order.verify(withdrawal).refund();
            verify(plugin, never()).logError(anyString());
            tasks.verify(() -> TaskScheduler.user(any(), any(), any()), never());
        }
    }

    @Test void unpaidEventCancellationIsNotAPaymentFailureAndAllowsRetry() {
        when(builder.buildAsync()).thenReturn(CompletableFuture.completedFuture(null));
        var completed = mock(Runnable.class);
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            for (int attempt = 0; attempt < 2; attempt++) {
                assertTrue(BlueprintCostHelper.build(plugin, addon, user, "paid", world, builder, false,
                        () -> CompletableFuture.completedFuture(null), completed));
            }
            verify(builder, times(2)).buildAsync();
            verifyNoInteractions(withdrawal, completed);
            verify(plugin, never()).logError(anyString());
            tasks.verify(() -> TaskScheduler.user(any(), any(), any()), never());
            assertFalse(java.nio.file.Files.exists(directory.resolve("blueprint-payments")));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"rejected", "null", "exception"})
    void paidCancellationWithUnconfirmedRefundRemainsAnErrorAndCannotRetry(String outcome) throws Exception {
        when(builder.buildAsync()).thenReturn(CompletableFuture.completedFuture(null));
        switch (outcome) {
        case "rejected" -> when(withdrawal.refund()).thenReturn(new EconomyResponse(0, 0, ResponseType.FAILURE, "rejected"));
        case "null" -> when(withdrawal.refund()).thenReturn(null);
        default -> when(withdrawal.refund()).thenThrow(new IllegalStateException("unknown refund"));
        }
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(build());
            verify(plugin).logError(contains("Blueprint creation/payment did not complete"));
            tasks.verify(() -> TaskScheduler.user(eq(plugin), eq(user), any()));
            try (var files = java.nio.file.Files.list(directory.resolve("blueprint-payments"));
                    var input = java.nio.file.Files.newInputStream(files.findFirst().orElseThrow())) {
                var receipt = new java.util.Properties();
                receipt.load(input);
                assertEquals("REFUND_UNKNOWN", receipt.getProperty("state"));
            }
            assertTrue(build());
            verify(withdrawal).get();
            verify(withdrawal).refund();
            verify(builder).buildAsync();
        }
    }

    @Test void unpaidBuildExceptionStillReportsFailure() {
        when(builder.buildAsync()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("paste failed")));
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(BlueprintCostHelper.build(plugin, addon, user, "paid", world, builder, false,
                    () -> CompletableFuture.completedFuture(null), () -> fail("Must not complete")));
            verify(plugin).logError(contains("paste failed"));
            tasks.verify(() -> TaskScheduler.user(eq(plugin), eq(user), any()));
        }
    }

    @Test void beforeTeleportVetoAfterCommittingRefundsWithoutCompletingPayment() throws Exception {
        var beforeTeleport = new java.util.concurrent.atomic.AtomicReference<java.util.function.Supplier<CompletableFuture<Void>>>();
        when(builder.beforeTeleport(any())).thenAnswer(call -> {
            beforeTeleport.set(call.getArgument(0));
            return builder;
        });
        when(builder.buildAsync()).thenAnswer(call -> beforeTeleport.get().get().handle((done, error) -> {
            assertNotNull(error);
            return null; // NewIsland returns null only after the vetoed draft was safely discarded.
        }));
        var completed = mock(Runnable.class);
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(BlueprintCostHelper.build(plugin, addon, user, "paid", world, builder, true,
                    () -> CompletableFuture.failedFuture(new java.util.concurrent.CancellationException("preclear veto")), completed));
            verify(withdrawal).get();
            verify(withdrawal).refund();
            verifyNoInteractions(completed);
            verify(plugin, never()).logError(anyString());
            tasks.verify(() -> TaskScheduler.user(any(), any(), any()), never());
            try (var files = java.nio.file.Files.list(directory.resolve("blueprint-payments"));
                    var input = java.nio.file.Files.newInputStream(files.findFirst().orElseThrow())) {
                var receipt = new java.util.Properties();
                receipt.load(input);
                assertEquals("REFUNDED", receipt.getProperty("state"));
            }
        }
    }

    @Test void rejectedTeleportAfterCommittingPreservesPaymentForReview() throws Exception {
        var beforeTeleport = new java.util.concurrent.atomic.AtomicReference<java.util.function.Supplier<CompletableFuture<Void>>>();
        when(builder.beforeTeleport(any())).thenAnswer(call -> {
            beforeTeleport.set(call.getArgument(0));
            return builder;
        });
        when(builder.buildAsync()).thenAnswer(call -> beforeTeleport.get().get().thenCompose(done ->
                CompletableFuture.failedFuture(new IllegalStateException("Home teleport was rejected"))));
        var completed = mock(Runnable.class);
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(BlueprintCostHelper.build(plugin, addon, user, "paid", world, builder, true,
                    () -> CompletableFuture.completedFuture(null), completed));
            verify(withdrawal, never()).refund();
            verifyNoInteractions(completed);
            try (var files = java.nio.file.Files.list(directory.resolve("blueprint-payments"));
                    var input = java.nio.file.Files.newInputStream(files.findFirst().orElseThrow())) {
                var receipt = new java.util.Properties();
                receipt.load(input);
                assertEquals("COMMITTING", receipt.getProperty("state"));
            }
        }
    }

    @Test void unknownDebitSurvivesRestartAndNeverBuildsOrRetries() {
        when(withdrawal.get()).thenThrow(new IllegalStateException("provider changed balance then threw"));
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(build());
            assertTrue(build());
            verify(withdrawal).get();
            verify(withdrawal, never()).refund();
            verify(builder, never()).buildAsync();
        }
    }

    @Test void partialCommitNeverRefundsOrRetries() {
        when(builder.buildAsync()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("cleanup partially committed")));
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(build());
            assertTrue(build());
            verify(withdrawal).get();
            verify(withdrawal, never()).refund();
            verify(builder).buildAsync();
        }
    }

    @Test void confirmedRollbackRefunds() {
        when(builder.buildAsync()).thenReturn(CompletableFuture.failedFuture(new NewIsland.RolledBackException(new Exception("paste failed"))));
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            assertTrue(build());
            verify(withdrawal).refund();
            verify(plugin).logError(contains("paste failed"));
        }
    }

    @Test void missingProviderDoesNotDeliverPaidBlueprint() {
        when(plugin.getVault()).thenReturn(Optional.empty());
        assertFalse(build());
        verify(builder, never()).buildAsync();
    }

    @Test void shutdownBeforePlayerTaskRefundsWithoutBuilding() {
        try (var tasks = mockStatic(TaskScheduler.class)) {
            immediate(tasks);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("retired")));
            assertTrue(build());
            verify(withdrawal).refund();
            verify(builder, never()).buildAsync();
        }
    }

    static void immediate(org.mockito.MockedStatic<TaskScheduler> tasks) {
        tasks.when(() -> TaskScheduler.globalFuture(any(), any())).thenAnswer(call -> run(call.getArgument(1)));
        tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> run(call.getArgument(2)));
    }

    private static CompletableFuture<Void> run(Runnable action) {
        try { action.run(); return CompletableFuture.completedFuture(null); }
        catch (Throwable error) { return CompletableFuture.failedFuture(error); }
    }
}
