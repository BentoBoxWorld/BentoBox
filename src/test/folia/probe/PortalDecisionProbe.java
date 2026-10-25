package probe;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.api.user.User;

/** Disposable-server only: creates fresh owned fixtures; never edits existing islands or configuration. */
public final class PortalDecisionProbe implements Listener {
    private final FoliaProbe plugin;
    private volatile Player bot;
    private volatile String phase;
    private volatile Location redirect;
    private volatile Island island;
    private final AtomicInteger synthetic = new AtomicInteger();
    private final AtomicInteger nativePre = new AtomicInteger();
    private final AtomicInteger arrivals = new AtomicInteger();
    private final AtomicInteger portalCreates = new AtomicInteger();
    private final AtomicInteger locate = new AtomicInteger();
    private volatile Throwable listenerFailure;
    private volatile PlayerPortalEvent dispatchCheck;
    private volatile boolean dispatchObserved;

    public PortalDecisionProbe(FoliaProbe plugin) { this.plugin = plugin; }

    public static CompletableFuture<Void> run(FoliaProbe plugin) {
        return new PortalDecisionProbe(plugin).run();
    }

    public CompletableFuture<Void> run() {
        CompletableFuture<Void> result;
        try {
            var bb = BentoBox.getInstance();
            World world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
            require(bb.getIWM().isNetherIslands(world), "Nether islands must be enabled");
            require(bb.getIWM().getWorldSettings(world).isMakeNetherPortals(), "Temporarily enable make-nether-portals");
            require(bb.getIWM().isPasteMissingIslands(world), "Temporarily enable create-missing-nether-end-islands");
            require(!bb.getIWM().isUseOwnGenerator(world), "Temporarily disable use-own-generator for missing-island path");
            register();
            boolean nativeTarget = supportsNativeTarget(
                    Class.forName("me.earthme.luminol.api.entity.PreEntityPortalEvent"));
            result = scenario(world, "synthetic-cancel");
            if (nativeTarget) {
                result = result.thenCompose(unused -> scenario(world, "native-cancel"))
                        .thenCompose(unused -> scenario(world, "redirect-missing"));
            } else {
                // This core's pre-event has no setters, so BentoBox always cancels it and routes the
                // transfer itself. A third party can never see it uncancelled, and there is no target
                // to forward, so these two scenarios cannot exist on this build.
                plugin.getLogger().info("PORTAL_DECISION_NATIVE_SCENARIOS_SKIPPED: core pre-event has no "
                        + "target/radius setters; BentoBox routes these transfers itself");
            }
        } catch (Throwable error) {
            result = CompletableFuture.failedFuture(error);
        }
        return result.orTimeout(240, TimeUnit.SECONDS).whenComplete((unused, error) -> {
            HandlerList.unregisterAll(this);
            Player last = bot;
            if (last != null) despawn(last);
            if (error == null) plugin.getLogger().info("PORTAL_DECISION_PROBE_PASSED: late synthetic cancel; "
                    + "scenarios that need a core target override were skipped when unsupported");
            else plugin.getLogger().log(Level.SEVERE, "PORTAL_DECISION_PROBE_FAILED phase=" + phase, error);
            plugin.getLogger().info("PORTAL_DECISION_SCOPE: no cross-world redirect or exhaustive radius boundary proof; "
                    + "only fresh test islands modified; no configuration saved");
        });
    }

    private void register() throws Exception {
        Bukkit.getPluginManager().registerEvent(PortalCreateEvent.class, this, EventPriority.MONITOR, (ignored, event) -> {
            var create = (PortalCreateEvent) event;
            if (create.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR || create.isCancelled()) return;
            Island current = island;
            if (current == null || create.getBlocks().stream().noneMatch(block -> current.inIslandSpace(block.getLocation()))) return;
            portalCreates.incrementAndGet();
            plugin.getLogger().info("PORTAL_DECISION_NATIVE_CREATE: phase=" + phase + " world=" + create.getWorld().getName()
                    + " blocks=" + create.getBlocks().size());
        }, plugin);
        Bukkit.getPluginManager().registerEvent(PlayerTeleportEvent.class, this, EventPriority.MONITOR, (ignored, event) -> {
            var teleport = (PlayerTeleportEvent) event;
            if (owns(teleport.getPlayer())) plugin.getLogger().info("PORTAL_DECISION_TELEPORT_FINAL: phase=" + phase
                    + " cause=" + teleport.getCause() + " cancelled=" + teleport.isCancelled()
                    + " from=" + teleport.getFrom() + " to=" + teleport.getTo());
        }, plugin);
        Bukkit.getPluginManager().registerEvent(PlayerPortalEvent.class, this, EventPriority.HIGHEST, (ignored, event) -> {
            PlayerPortalEvent portal = (PlayerPortalEvent) event;
            if (portal == dispatchCheck) { dispatchObserved = true; return; }
            if (portal.getPlayer().getName().startsWith("PD")) plugin.getLogger().info("PORTAL_DECISION_SYNTHETIC_MATCH: player="
                    + portal.getPlayer().getUniqueId() + " tracked=" + (bot == null ? null : bot.getUniqueId()));
            if (!owns(portal.getPlayer())) return;
            synthetic.incrementAndGet();
            plugin.getLogger().info("PORTAL_DECISION_SYNTHETIC: phase=" + phase + " player=" + portal.getPlayer().getUniqueId()
                    + " beforeCancelled=" + portal.isCancelled() + " to=" + portal.getTo()
                    + " thread=" + Thread.currentThread().getName());
            if (portal.isCancelled()) {
                listenerFailure = new IllegalStateException("Portal was already cancelled before the late-decision probe");
                return;
            }
            if (phase.equals("synthetic-cancel")) portal.setCancelled(true);
            if (phase.equals("redirect-missing")) {
                portal.setTo(redirect.clone());
                portal.setSearchRadius(0);
                portal.setCreationRadius(2);
                portal.setCanCreatePortal(true);
            }
            plugin.getLogger().info("PORTAL_DECISION_SYNTHETIC_DECIDED: phase=" + phase + " cancelled=" + portal.isCancelled());
        }, plugin);
        registerNative("me.earthme.luminol.api.entity.PreEntityPortalEvent", (event, type) -> {
            boolean nativeTarget = supportsNativeTarget(type);
            plugin.getLogger().info("PORTAL_DECISION_PRE_MATCH: entity=" + type.getMethod("getEntity").invoke(event)
                    + " tracked=" + (bot == null ? null : bot.getUniqueId()));
            if (!owns(type.getMethod("getEntity").invoke(event))) return;
            nativePre.incrementAndGet();
            plugin.getLogger().info("PORTAL_DECISION_PRE: phase=" + phase + " synthetic=" + synthetic.get()
                    + " cancelled=" + ((Cancellable) event).isCancelled()
                    + " target=" + (nativeTarget ? type.getMethod("getPortalTarget").invoke(event) : "unsupported")
                    + " thread=" + Thread.currentThread().getName());
            require(synthetic.get() > 0, "Native pre-event did not dispatch the probe's synthetic listener");
            if (phase.equals("synthetic-cancel")) require(((Cancellable) event).isCancelled(),
                    "Late synthetic cancellation was not propagated to native pre-event");
            if (!phase.equals("synthetic-cancel")) require(!((Cancellable) event).isCancelled(),
                    "Native portal was already cancelled before the late-decision probe");
            if (phase.equals("native-cancel")) ((Cancellable) event).setCancelled(true);
            if (phase.equals("redirect-missing")) {
                if (!nativeTarget) {
                    // This core has no portal target/radius override, so BentoBox routes the
                    // transfer itself; only the manual arrival can be checked.
                    plugin.getLogger().info("PORTAL_DECISION_NATIVE_TARGET_UNSUPPORTED: core has no redirect API");
                    return;
                }
                require(redirect.equals(type.getMethod("getPortalTarget").invoke(event)), "Final redirected target not forwarded");
                require(redirect.getWorld().equals(type.getMethod("getDestination").invoke(event)), "Final world not forwarded");
                require(((Number) type.getMethod("getSearchRadius").invoke(event)).intValue() == 0, "Search radius not forwarded");
                require(((Number) type.getMethod("getCreationRadius").invoke(event)).intValue() == 2, "Creation radius not forwarded");
                require(type.getMethod("getPortalReady").invoke(event) != null, "Missing-island continuation absent");
            }
            if (listenerFailure != null && event instanceof Cancellable cancel) cancel.setCancelled(true);
        });
        registerFinalCancellation();
        registerNative("me.earthme.luminol.api.entity.PostEntityPortalEvent", (event, type) -> {
            Object entity = type.getMethod("getTeleportedEntity").invoke(event);
            plugin.getLogger().info("PORTAL_DECISION_POST: phase=" + phase + " entity=" + entity + " tracked=" + owns(entity));
            // This core exposes the API but never emits PostEntityPortalEvent; diagnostic only.
        });
        registerNative("me.earthme.luminol.api.portal.PortalLocateEvent", (event, type) -> {
            Location from = (Location) type.getMethod("getOriginal").invoke(event);
            Island current = island;
            if (current == null || !current.inIslandSpace(from)) return;
            plugin.getLogger().info("PORTAL_DECISION_LOCATE: phase=" + phase + " synthetic=" + synthetic.get()
                    + " pre=" + nativePre.get() + " original=" + from + " target=" + type.getMethod("getDestination").invoke(event));
            locate.incrementAndGet();
            require(phase.equals("redirect-missing"), "Native locate ran after cancellation");
            Location target = (Location) type.getMethod("getDestination").invoke(event);
            require(target.getWorld().equals(redirect.getWorld()) && target.distanceSquared(redirect) < 1,
                    "Core native locate did not receive redirect: " + target);
        });
    }

    private interface NativeCheck { void accept(Event event, Class<?> type) throws Exception; }

    /**
     * Older Luminol builds expose only the base event: no target, radius or continuation
     * accessors. BentoBox detects that too and routes such transfers itself.
     */
    private static boolean supportsNativeTarget(Class<?> type) {
        for (String name : new String[] {"getPortalTarget", "getSearchRadius", "getCreationRadius", "getPortalReady"}) {
            try {
                type.getMethod(name);
            } catch (NoSuchMethodException missing) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void registerFinalCancellation() throws Exception {
        Class<? extends Event> type = (Class<? extends Event>) Class.forName("me.earthme.luminol.api.entity.PreEntityPortalEvent");
        Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR, (ignored, event) -> {
            try {
                if (!owns(type.getMethod("getEntity").invoke(event))) return;
                boolean cancelled = ((Cancellable) event).isCancelled();
                plugin.getLogger().info("PORTAL_DECISION_PRE_FINAL: phase=" + phase + " cancelled=" + cancelled);
                require(phase.equals("redirect-missing") || cancelled, "Final native cancellation was cleared after the probe");
            } catch (Throwable error) { listenerFailure = error; }
        }, plugin);
    }

    @SuppressWarnings("unchecked")
    private void registerNative(String name, NativeCheck check) throws Exception {
        Class<? extends Event> type = (Class<? extends Event>) Class.forName(name);
        Bukkit.getPluginManager().registerEvent(type, this, EventPriority.HIGHEST, (ignored, event) -> {
            try { check.accept(event, type); }
            catch (Throwable error) {
                listenerFailure = error;
                if (event instanceof Cancellable cancel) cancel.setCancelled(true);
            }
        }, plugin);
    }

    private boolean owns(Object player) {
        return bot != null && player instanceof org.bukkit.entity.Entity entity
                && bot.getUniqueId().equals(entity.getUniqueId());
    }

    private CompletableFuture<Void> scenario(World world, String name) {
        phase = name;
        synthetic.set(0); nativePre.set(0); arrivals.set(0); locate.set(0); portalCreates.set(0); listenerFailure = null;
        int coordinate = 19_000_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(5000) * 400;
        Location frame = new Location(world, coordinate + 4, 90, coordinate + 8);
        World nether = Objects.requireNonNull(BentoBox.getInstance().getIWM().getNetherWorld(world));
        Location center = new Location(world, coordinate, 90, coordinate);
        redirect = new Location(nether, coordinate + 49, 91, coordinate + 49);
        var bb = BentoBox.getInstance();
        require(bb.getIslands().getIslandAt(center).isEmpty(), "Fixture overlaps an existing island");
        require(bb.getIslands().getIslandAt(frame).isEmpty() && bb.getIslands().getIslandAt(redirect).isEmpty(),
                "Portal fixture overlaps an existing island");
        plugin.getLogger().info("PORTAL_DECISION_BEGIN: " + name + " center=" + center + " redirect=" + redirect);
        return Util.getChunkAtAsync(frame.clone().add(0, 0, -4))
                .thenCompose(unused -> Util.getChunkAtAsync(frame))
                .thenCompose(unused -> TaskScheduler.regionFuture(plugin, frame, () -> invoke("buildNetherFrame",
                        new Class<?>[]{World.class, Location.class}, world, frame)))
                .thenCompose(unused -> spawn("PD" + UUID.randomUUID().toString().substring(0, 8), frame.clone().add(1.5, 1, -2)))
                .thenCompose(player -> {
                    bot = player;
                    island = Objects.requireNonNull(bb.getIslands().createIsland(center, player.getUniqueId()), "Island rejected");
                    require(island.inIslandSpace(redirect), "Redirect outside fresh test island");
                    return checkPlayerEventDispatch(player).thenCompose(unused -> island.hasNetherIslandAsync());
                }).thenCompose(exists -> {
                    require(!exists, "Fresh fixture already has Nether center terrain; cannot prove missing-island paste");
                    return name.equals("redirect-missing") ? targetFloor() : CompletableFuture.completedFuture(null);
                }).thenCompose(unused -> {
                    return bot.teleportAsync(frame.clone().add(1.5, 1, 0.5));
                }).thenCompose(success -> {
                    require(success, "Could not position bot in real portal");
                    return watch(world);
                }).thenCompose(unused -> {
                    require(listenerFailure == null, "Event assertion: " + listenerFailure);
                    require(synthetic.get() > 0 && nativePre.get() > 0, "Real PortalProcessor did not dispatch both events");
                    if (!name.equals("redirect-missing")) {
                        require(arrivals.get() == 0 && locate.get() == 0 && portalCreates.get() == 0,
                                "Cancelled transfer reached native locate/create/arrival");
                        return island.hasNetherIslandAsync().thenAccept(exists -> require(!exists, "Cancelled event still pasted island"));
                    }
                    require(arrivals.get() == 1 && locate.get() == 1, "Expected one native locate and arrival, got " + locate + "/" + arrivals);
                    require(portalCreates.get() == 1, "Expected one successful native paired-portal creation");
                    return island.hasNetherIslandAsync().thenAccept(exists -> require(exists, "Missing Nether blueprint was not pasted"));
                }).thenCompose(unused -> TaskScheduler.userFuture(plugin,
                        User.getInstance(bot), () -> {
                            if (name.equals("redirect-missing")) {
                                require(bot.getWorld().equals(nether), "Wrong arrival world");
                                require(bot.getLocation().distanceSquared(redirect) < 100, "Native arrival ignored redirected center");
                                require(bot.getLocation().getBlock().getType() == Material.NETHER_PORTAL, "Arrival did not use native portal");
                            } else require(bot.getWorld().equals(world), "Cancelled player changed world");
                            plugin.getLogger().info("PORTAL_DECISION_CASE_PASSED: " + name + " at=" + bot.getLocation());
                        })).thenCompose(unused -> despawn(bot)).whenComplete((unused, error) -> { if (error == null) bot = null; });
    }

    private CompletableFuture<Void> watch(World source) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicInteger ticks = new AtomicInteger();
        bot.getScheduler().runAtFixedRate(plugin, task -> {
            try {
                if (listenerFailure != null) throw new IllegalStateException("Event assertion", listenerFailure);
                int elapsed = ticks.addAndGet(10);
                if (elapsed % 100 == 0) plugin.getLogger().info("PORTAL_DECISION_WATCH: phase=" + phase
                        + " ticks=" + elapsed + " at=" + bot.getLocation() + " valid=" + bot.isValid()
                        + " block=" + bot.getLocation().getBlock().getType() + " cooldown=" + bot.getPortalCooldown()
                        + " mode=" + bot.getGameMode() + " dead=" + bot.isDead()
                        + " synthetic=" + synthetic.get() + " pre=" + nativePre.get()
                        + " locate=" + locate.get() + " arrivals=" + arrivals.get());
                if (phase.equals("redirect-missing")) {
                    if (!bot.getWorld().equals(source)) {
                        require(bot.isValid() && bot.isOnline(), "Arrived fixture is not live");
                        require(bot.getWorld().equals(redirect.getWorld()) && bot.getLocation().distanceSquared(redirect) < 100,
                                "Arrival outside redirected portal target");
                        require(bot.getLocation().getBlock().getType() == Material.NETHER_PORTAL,
                                "Arrival did not use a native portal block");
                        require(bot.getPortalCooldown() > 0, "Native arrival portal cooldown missing");
                        require(locate.get() == 1 && portalCreates.get() == 1, "Arrival missing native locate/create evidence");
                        arrivals.incrementAndGet();
                        plugin.getLogger().info("PORTAL_DECISION_ARRIVAL_ATTESTED: player=" + bot.getUniqueId()
                                + " at=" + bot.getLocation() + " cooldown=" + bot.getPortalCooldown());
                        result.complete(null); task.cancel();
                    }
                } else if (nativePre.get() > 0 && elapsed >= 180) { result.complete(null); task.cancel(); }
                if (elapsed >= 1200) throw new IllegalStateException("Portal decision timeout");
            } catch (Throwable error) { result.completeExceptionally(error); task.cancel(); }
        }, () -> result.completeExceptionally(new IllegalStateException("Bot retired")), 10, 10);
        return result.orTimeout(90, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> targetFloor() {
        var writes = new java.util.ArrayList<CompletableFuture<Void>>();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            Location column = redirect.clone().add(x, -1, z);
            require(island.inIslandSpace(column), "Target floor outside fresh island");
            writes.add(Util.getChunkAtAsync(column).thenCompose(chunk -> TaskScheduler.regionFuture(plugin, column, () -> {
                column.getBlock().setType(Material.OBSIDIAN, false);
                for (int y = 1; y <= 5; y++) column.clone().add(0, y, 0).getBlock().setType(Material.AIR, false);
            })));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Player> spawn(String name, Location location) {
        // Use the reviewed player fixture; this core suppresses Leaves Bot PlayerEvents.
        return (CompletableFuture<Player>) invoke("spawnFakePlayerPlugin",
                new Class<?>[]{String.class, World.class, Location.class}, name, location.getWorld(), location);
    }

    private CompletableFuture<Void> checkPlayerEventDispatch(Player player) {
        return TaskScheduler.userFuture(plugin, User.getInstance(player), () -> {
            dispatchObserved = false;
            dispatchCheck = new PlayerPortalEvent(player, player.getLocation(), player.getLocation(),
                    org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            dispatchCheck.setCancelled(true);
            try {
                Bukkit.getPluginManager().callEvent(dispatchCheck);
                require(dispatchObserved, "Core suppressed PlayerPortalEvent for fixture class " + player.getClass().getName());
                plugin.getLogger().info("PORTAL_DECISION_DISPATCH_PREFLIGHT_PASSED: fixture=" + player.getClass().getName()
                        + " thread=" + Thread.currentThread().getName());
            } finally { dispatchCheck = null; }
        });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> despawn(Player player) {
        return (CompletableFuture<Void>) invoke("despawnFakePlayerVerified", new Class<?>[]{Player.class}, player);
    }

    private Object invoke(String name, Class<?>[] types, Object... args) {
        try {
            Method method = FoliaProbe.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(plugin, args);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(name, error); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
