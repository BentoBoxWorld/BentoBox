package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.World;
import world.bentobox.bentobox.managers.island.NewIsland;

/**
 * Shared cost-checking logic for island create and reset commands.
 * Checks if the user can afford a blueprint bundle cost, and optionally charges them.
 * Cost is only applied when multiple bundles are available and economy is enabled.
 */
class BlueprintCostHelper {
    private static final java.util.Set<java.util.UUID> PAYING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private BlueprintCostHelper() {}

    /** Capture account/price on the player's thread; all Vault calls run on the global/main thread. */
    static boolean build(BentoBox plugin, GameModeAddon addon, User user, String name, World world,
            NewIsland.Builder builder, boolean charge, Supplier<CompletableFuture<Void>> beforeCommit, Runnable completed) {
        var bundles = plugin.getBlueprintsManager().getBlueprintBundles(addon);
        BlueprintBundle bundle = bundles.get(name);
        double amount = !charge || bundles.size() <= 1 || !plugin.getSettings().isUseEconomy()
                || bundle == null ? 0 : bundle.getCost();
        if (!Double.isFinite(amount) || amount < 0) {
            user.sendMessage("general.errors.general");
            return false;
        }
        var provider = amount > 0 ? plugin.getVault() : java.util.Optional.<world.bentobox.bentobox.hooks.VaultHook>empty();
        if (amount > 0 && provider.isEmpty()) {
            user.sendMessage("general.errors.general");
            return false;
        }
        var withdrawal = amount > 0 ? provider.orElseThrow().captureRefundableWithdrawal(user, amount, world) : null;
        var playerId = user.getUniqueId();
        if (!PAYING.add(playerId)) return false;
        String worldName = world == null ? null : world.getName();
        var settlement = new java.util.concurrent.atomic.AtomicReference<BlueprintSettlement>();
        var started = new java.util.concurrent.atomic.AtomicBoolean();
        TaskScheduler.globalFuture(plugin, () -> {
            if (withdrawal == null) return;
            try {
                var payment = new BlueprintSettlement(BlueprintSettlement.directory(plugin), playerId, worldName, name, amount, withdrawal);
                settlement.set(payment);
                if (!payment.debit()) throw new IllegalStateException("Blueprint withdrawal rejected or outcome unknown");
            } catch (IOException error) { throw new java.io.UncheckedIOException(error); }
        }).thenCompose(ignored -> onUser(plugin, user, () -> {
            started.set(true);
            return builder.transactional()
                .allocated(island -> TaskScheduler.globalFuture(plugin, () -> {
                    try { if (settlement.get() != null) settlement.get().allocated(island.getUniqueId()); }
                    catch (IOException error) { throw new java.io.UncheckedIOException(error); }
                }))
                .beforeTeleport(() -> TaskScheduler.globalFuture(plugin, () -> {
                    try { if (settlement.get() != null) settlement.get().committing(); }
                    catch (IOException error) { throw new java.io.UncheckedIOException(error); }
                }).thenCompose(done -> onUser(plugin, user, beforeCommit)))
                .afterBuild(island -> TaskScheduler.globalFuture(plugin, () -> {
                    try { if (settlement.get() != null) settlement.get().complete(island.getUniqueId()); }
                    catch (IOException error) { throw new java.io.UncheckedIOException(error); }
                    completed.run();
                }))
                .buildAsync();
        })).whenComplete((island, error) -> {
                    if (error == null && island != null) { PAYING.remove(playerId); return; }
                    boolean rolledBack = error == null || !started.get();
                    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                        if (cause instanceof NewIsland.RolledBackException) rolledBack = true;
                    }
                    final boolean safeToRefund = rolledBack;
                    TaskScheduler.globalFuture(plugin, () -> {
                        boolean compensated = settlement.get() == null;
                        try { if (settlement.get() != null) compensated = settlement.get().failed(safeToRefund); }
                        catch (Exception failure) { plugin.logStacktrace(failure); }
                        if (error == null && compensated) return; // A veto with a safely discarded draft is normal cancellation.
                        plugin.logError("Blueprint creation/payment did not complete for " + playerId
                                + "; inspect blueprint-payments receipts before retrying or refunding. " + error);
                        TaskScheduler.user(plugin, user, () -> user.sendMessage("commands.island.create.cannot-create-island"));
                    }).whenComplete((done, failure) -> PAYING.remove(playerId));
                });
        return true;
    }

    private static <T> CompletableFuture<T> onUser(BentoBox plugin, User user, Supplier<CompletableFuture<T>> action) {
        var result = new java.util.concurrent.atomic.AtomicReference<CompletableFuture<T>>();
        return TaskScheduler.userFuture(plugin, user, () -> result.set(action.get())).thenCompose(ignored -> result.get());
    }

}
