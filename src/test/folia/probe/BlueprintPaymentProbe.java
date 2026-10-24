package probe;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.island.IslandResetCommand;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.island.IslandPreclearEvent;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.util.TaskScheduler;

/** Disposable-server only. Uses configured paid terrain, real Vault and a newly spawned probe account.
 * Entry from FoliaProbe.ready: BlueprintPaymentProbe.run(this); return;
 * Does not cover command permissions, confirmation GUI, crash recovery or provider outages.
 */
public final class BlueprintPaymentProbe implements Listener {
    private final JavaPlugin host;
    private final BentoBox box = BentoBox.getInstance();
    private final AtomicInteger builds = new AtomicInteger();
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicReference<Throwable> eventFailure = new AtomicReference<>();
    private volatile UUID playerId;
    private String playerName;
    private volatile boolean cancelPreclear;
    private volatile boolean sawPreclear;
    private GameModeAddon addon;
    private World world;
    private User user;
    private VaultHook vault;
    private String bundle;
    private double cost;
    private double baseline;
    private Method build;
    private Method clearOld;
    private Object resetCommand;
    private volatile Path activeReceipt;

    private BlueprintPaymentProbe(JavaPlugin host) { this.host = host; }

    public static CompletableFuture<Void> run(JavaPlugin host) {
        var probe = new BlueprintPaymentProbe(host);
        Bukkit.getPluginManager().registerEvents(probe, host);
        CompletableFuture<Void> result;
        try { result = probe.start(); }
        catch (Throwable error) { result = CompletableFuture.failedFuture(error); }
        return result.orTimeout(300, TimeUnit.SECONDS).handle((ignored, error) -> {
            if (error == null) host.getLogger().info("BLUEPRINT_PAYMENT_PROBE_BUSINESS_PASSED: all five payment stages verified; exit not yet verified");
            return probe.verifyLogout().<Void>handle((loggedOut, logoutError) -> {
                    if (error != null) {
                        if (logoutError != null) error.addSuppressed(logoutError);
                        throw new java.util.concurrent.CompletionException(error);
                    }
                    if (logoutError != null) throw new java.util.concurrent.CompletionException(logoutError);
                    return null;
                });
        }).thenCompose(finished -> finished).whenComplete((ignored, error) -> {
            HandlerList.unregisterAll(probe);
            if (error != null) host.getLogger().log(Level.SEVERE, "BLUEPRINT_PAYMENT_PROBE_FAILED", error);
            else host.getLogger().info("BLUEPRINT_PAYMENT_PROBE_PASSED: real Vault debit-before-build, create/reset price delta, "
                    + "PRECLEAR refund, unfinished receipt and retry without debit; helper-level, not command/GUI coverage");
            host.getLogger().warning("Blueprint payment probe preserves islands and receipts for inspection; "
                    + "the final COMMITTING receipt is intentionally unresolved. player=" + probe.playerId);
        });
    }

    private CompletableFuture<Void> start() throws ReflectiveOperationException {
        world = java.util.Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"), "BSkyBlock world required");
        addon = box.getIWM().getAddon(world).orElseThrow();
        require(box.getSettings().isUseEconomy(), "Enable economy in the isolated-server fixture first");
        require(box.getSettings().isChargeForBlueprintOnReset(), "Reset blueprint cost must be enabled");
        require(!box.getIWM().isOnJoinResetMoney(world), "Money-on-join reset would invalidate exact price assertions");
        vault = box.getVault().orElseThrow(() -> new IllegalStateException("Real Vault provider required"));
        var bundles = box.getBlueprintsManager().getBlueprintBundles(addon);
        require(bundles.size() > 1, "At least two configured bundles required; probe does not alter configuration");
        bundle = System.getProperty("probe.paymentBundle", bundles.entrySet().stream()
                .filter(e -> Double.isFinite(e.getValue().getCost()) && e.getValue().getCost() > 0)
                .map(java.util.Map.Entry::getKey).sorted().findFirst().orElseThrow());
        require(bundles.containsKey(bundle), "Unknown probe.paymentBundle: " + bundle);
        cost = bundles.get(bundle).getCost();
        require(Double.isFinite(cost) && cost > 0 && Double.isFinite(cost * 10), "Finite positive bundle cost required");
        Class<?> helper = Class.forName("world.bentobox.bentobox.api.commands.island.BlueprintCostHelper", true,
                box.getClass().getClassLoader());
        build = helper.getDeclaredMethod("build", BentoBox.class, GameModeAddon.class, User.class, String.class,
                World.class, NewIsland.Builder.class, boolean.class, Supplier.class, Runnable.class);
        build.setAccessible(true);
        resetCommand = addon.getPlayerCommand().orElseThrow().getSubCommand("reset").orElseThrow();
        clearOld = IslandResetCommand.class.getDeclaredMethod("clearOldIslandAsync", User.class, Island.class);
        clearOld.setAccessible(true);
        String name = "Pay" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        require(Bukkit.getPlayerExact(name) == null && Bukkit.getOfflinePlayerIfCached(name) == null,
                "Probe name already exists");
        Method spawn = host.getClass().getDeclaredMethod("spawnFakePlayerPlugin", String.class, World.class, Location.class);
        spawn.setAccessible(true);
        playerName = name;
        @SuppressWarnings("unchecked")
        var bot = (CompletableFuture<Player>) spawn.invoke(host, name, world, new Location(world, 8192.5, 90, 8192.5));
        return bot.thenCompose(player -> {
            playerId = player.getUniqueId();
            user = User.getInstance(player);
            return TaskScheduler.userFuture(host, user, () -> {
                player.setInvulnerable(true);
                player.setGravity(false);
                require(!box.getIslands().hasIsland(world, user), "New probe fakeplayer already owns an island");
            });
        }).thenCompose(ignored -> TaskScheduler.globalFuture(host, () -> {
            require(receipts().isEmpty(), "New probe fakeplayer already has payment history");
            try {
                Object provider = vault.getClass().getMethod("getEconomy").invoke(vault);
                Class<?> economy = Class.forName("net.milkbowl.vault.economy.Economy", true, vault.getClass().getClassLoader());
                economy.getMethod("createPlayerAccount", org.bukkit.OfflinePlayer.class).invoke(provider, user.getOfflinePlayer());
                Object funded = vault.getClass().getMethod("deposit", User.class, double.class, World.class)
                        .invoke(vault, user, Math.max(1000, cost * 10), world);
                require(Boolean.TRUE.equals(funded.getClass().getMethod("transactionSuccess").invoke(funded)), "Vault funding failed");
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            baseline = balance();
            host.getLogger().info("BLUEPRINT_PAYMENT_FIXTURE: player=" + playerId + " bundle=" + bundle + " cost=" + cost);
        })).thenCompose(ignored -> attempt(null, () -> CompletableFuture.completedFuture(null)))
                .thenCompose(ignored -> verify(1, 1, baseline - cost, "COMPLETED"))
                .thenCompose(ignored -> onUser(() -> {
                    Island old = currentIsland();
                    cancelPreclear = true;
                    sawPreclear = false;
                    return attempt(old, () -> cleanup(old)).thenCompose(done -> verify(2, 1, baseline - cost, "REFUNDED"))
                            .thenCompose(done -> TaskScheduler.userFuture(host, user, () -> {
                                require(sawPreclear && !cancelPreclear, "Real PRECLEAR cancellation was not observed");
                                require(currentIsland().getUniqueId().equals(old.getUniqueId()), "Cancelled reset replaced old island");
                                require(playerId.equals(old.getOwner()), "Cancelled reset changed old ownership");
                            }));
                }))
                .thenCompose(ignored -> onUser(() -> {
                    Island old = currentIsland();
                    return attempt(old, () -> cleanup(old)).thenCompose(done -> verify(3, 2, baseline - 2 * cost, "COMPLETED"))
                            .thenCompose(done -> TaskScheduler.userFuture(host, user, () -> {
                                require(!currentIsland().getUniqueId().equals(old.getUniqueId()), "Successful reset did not replace island");
                                require(!old.isOwned(), "Successful reset retained old owner");
                            }));
                }))
                .thenCompose(ignored -> onUser(() -> {
                    Island old = currentIsland();
                    return attempt(old, () -> CompletableFuture.failedFuture(
                            new IllegalStateException("probe injected pre-commit cleanup failure")))
                            .thenCompose(done -> TaskScheduler.userFuture(host, user, () -> {
                                require(currentIsland().getUniqueId().equals(old.getUniqueId()), "Failed reset replaced old island");
                                require(playerId.equals(old.getOwner()), "Failed reset changed old ownership");
                            }));
                }))
                .thenCompose(ignored -> verify(4, 2, baseline - 3 * cost, "COMMITTING"))
                .thenCompose(ignored -> onUser(() -> attempt(currentIsland(), () -> CompletableFuture.completedFuture(null))))
                .thenCompose(ignored -> verify(4, 2, baseline - 3 * cost, "COMMITTING"));
    }

    private CompletableFuture<Void> attempt(Island old, Supplier<CompletableFuture<Void>> beforeCommit) {
        return TaskScheduler.userFuture(host, user, () -> {
            var builder = NewIsland.builder().player(user).addon(addon).name(bundle).reason(old == null ? Reason.CREATE : Reason.RESET);
            if (old != null) builder.oldIsland(old);
            Supplier<CompletableFuture<Void>> checkedCommit = () -> TaskScheduler.globalFuture(host, () -> {
                require(eventFailure.get() == null, "Build event observed payment ordering failure: " + eventFailure.get());
                require(lastReceipt().getProperty("state").equals("COMMITTING"), "Commit receipt not durable before cleanup");
                assertBalance(baseline - (completed.get() + 1) * cost);
            }).thenCompose(done -> onUser(beforeCommit));
            require(Boolean.TRUE.equals(invoke(build, null, box, addon, user, bundle, world, builder, true,
                    checkedCommit, (Runnable) completed::incrementAndGet)), "Payment helper rejected request synchronously");
        }).thenCompose(ignored -> awaitIdle());
    }

    private CompletableFuture<Void> cleanup(Island old) {
        @SuppressWarnings("unchecked")
        var result = (CompletableFuture<Void>) invoke(clearOld, resetCommand, user, old);
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void creating(IslandCreateEvent event) { observeBuild(event.getPlayerUUID()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void resetting(IslandResetEvent event) { observeBuild(event.getPlayerUUID()); }

    private void observeBuild(UUID id) {
        if (!id.equals(playerId)) return;
        builds.incrementAndGet();
        try {
            var debited = receipts().stream().filter(p -> "DEBITED".equals(p.getProperty("state"))).toList();
            require(debited.size() == 1, "Expected exactly one debited receipt before build");
            activeReceipt = Path.of(debited.getFirst().getProperty("probe-file"));
            require(lastReceipt().getProperty("state").equals("DEBITED"), "Island build began before durable debit");
        } catch (Throwable error) { eventFailure.compareAndSet(null, error); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void preclear(IslandPreclearEvent event) {
        if (!event.getPlayerUUID().equals(playerId) || !cancelPreclear) return;
        sawPreclear = true;
        cancelPreclear = false;
        event.setCancelled(true);
    }

    private CompletableFuture<Void> verify(int expectedBuilds, int expectedCompleted, double expectedBalance, String state) {
        return TaskScheduler.globalFuture(host, () -> {
            require(eventFailure.get() == null, "Payment ordering assertion failed: " + eventFailure.get());
            require(builds.get() == expectedBuilds, "Unexpected build count: " + builds.get());
            require(completed.get() == expectedCompleted, "Failed/cancelled build marked completed: " + completed.get());
            int expectedReceipts = expectedBuilds;
            require(receipts().size() == expectedReceipts, "Retry produced an extra receipt");
            require(lastReceipt().getProperty("state").equals(state), "Unexpected receipt state: " + lastReceipt());
            assertBalance(expectedBalance);
            host.getLogger().info("BLUEPRINT_PAYMENT_STAGE_PASSED: builds=" + builds + " completed=" + completed
                    + " state=" + state + " balance=" + expectedBalance);
        });
    }

    private CompletableFuture<Void> awaitIdle() {
        var result = new CompletableFuture<Void>();
        // Poll the helper guard, not a timing guess: it is released after receipt/refund finalization.
        var task = TaskScheduler.globalTimer(host, () -> {
            if (result.isDone()) return;
            try {
                var field = build.getDeclaringClass().getDeclaredField("PAYING");
                field.setAccessible(true);
                if (!((java.util.Set<?>) field.get(null)).contains(playerId)) result.complete(null);
            } catch (Throwable error) { result.completeExceptionally(error); }
        }, 1, 2);
        return result.orTimeout(90, TimeUnit.SECONDS).whenComplete((done, error) -> task.cancel());
    }

    private CompletableFuture<Void> verifyLogout() {
        if (playerName == null) return CompletableFuture.completedFuture(null);
        try {
            java.util.Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("FakePlayerPlugin"),
                     "Reviewed FakePlayerPlugin required for verified probe logout");
            var player = java.util.Objects.requireNonNull(Bukkit.getPlayer(playerId),
                    "Probe player absent before native removal was requested");
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object listener = handle.getClass().getField("connection").get(handle);
            var receipt = listener.getClass();
            java.lang.reflect.Field removalField = null;
            for (Class<?> type = receipt; type != null && removalField == null; type = type.getSuperclass()) {
                try { removalField = type.getDeclaredField("carpetPlayerRemoval"); }
                catch (NoSuchFieldException ignored) { /* Declared on a core superclass. */ }
            }
            require(removalField != null, "Core native removal receipt missing");
            removalField.setAccessible(true);
            final var field = removalField;
            var removal = new CompletableFuture<Void>();
            var attached = new java.util.concurrent.atomic.AtomicBoolean();
            var poll = TaskScheduler.globalTimer(host, () -> {
                if (removal.isDone()) return;
                try {
                    Object value = field.get(listener);
                    if (value instanceof CompletableFuture<?> nativeRemoval && attached.compareAndSet(false, true)) {
                        nativeRemoval.whenComplete((done, failure) -> {
                            if (failure == null) removal.complete(null);
                            else removal.completeExceptionally(failure);
                        });
                    }
                } catch (Throwable failure) { removal.completeExceptionally(failure); }
            }, 1, 2);
            TaskScheduler.global(host, () -> {
                try {
                    require(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fpp despawn " + playerName),
                            "Fakeplayer despawn command rejected");
                } catch (Throwable failure) { removal.completeExceptionally(failure); }
            });
            // Core removal completion and disk-content attestation are separate checks.
            return removal.orTimeout(60, TimeUnit.SECONDS).whenComplete((done, failure) -> poll.cancel())
                    .thenCompose(ignored -> TaskScheduler.globalFuture(host, () -> {
                require(!player.isOnline() && Bukkit.getPlayer(playerId) == null,
                        "Native removal completed but probe player is still online");
                host.getLogger().info("BLUEPRINT_PAYMENT_NATIVE_REMOVAL_PASSED: player=" + playerId
                        + "; disk contents require independent readback");
            }));
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletableFuture<Void> onUser(Supplier<CompletableFuture<Void>> action) {
        var future = new AtomicReference<CompletableFuture<Void>>();
        return TaskScheduler.userFuture(host, user, () -> future.set(action.get())).thenCompose(ignored -> future.get());
    }

    private Island currentIsland() {
        return java.util.Objects.requireNonNull(box.getIslands().getIsland(world, user), "Probe island missing");
    }

    private List<Properties> receipts() {
        var directory = box.getDataFolder().toPath().resolve("blueprint-payments");
        if (!Files.isDirectory(directory)) return List.of();
        var result = new ArrayList<Properties>();
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.toString().endsWith(".properties")).toList()) {
                var receipt = new Properties();
                try (var input = Files.newInputStream(file)) { receipt.load(input); }
                if (String.valueOf(playerId).equals(receipt.getProperty("player"))) {
                    receipt.setProperty("probe-file", file.toString());
                    result.add(receipt);
                }
            }
        } catch (IOException error) { throw new IllegalStateException(error); }
        return result;
    }

    private Properties lastReceipt() {
        var receipt = new Properties();
        require(activeReceipt != null, "No payment receipt observed at build event");
        try (var input = Files.newInputStream(activeReceipt)) { receipt.load(input); }
        catch (IOException error) { throw new IllegalStateException(error); }
        return receipt;
    }

    private double balance() { return vault.getBalance(user, world); }

    private void assertBalance(double expected) {
        double actual = balance();
        require(Math.abs(actual - expected) < 0.000001, "Vault balance mismatch: expected=" + expected + " actual=" + actual);
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException error) { throw new IllegalStateException(error.getCause()); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
