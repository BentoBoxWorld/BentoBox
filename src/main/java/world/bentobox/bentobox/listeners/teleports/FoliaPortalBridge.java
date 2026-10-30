package world.bentobox.bentobox.listeners.teleports;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.plugin.EventExecutor;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport;

/**
 * Folia never fires {@link org.bukkit.event.player.PlayerPortalEvent}. Its portal code is
 * asynchronous and hands the entity straight to the server's default nether or end, so
 * BentoBox worlds would always leak into {@code minecraft:the_nether} / {@code minecraft:the_end}.
 * <p>
 * The Luminol family (Lophine) adds {@code me.earthme.luminol.api.entity.PreEntityPortalEvent},
 * which fires before the native transfer and can be cancelled. Linked Nether islands keep
 * native portal search/creation after BentoBox checks; other player transfers are handed
 * to {@link PlayerTeleportListener} for island-spawn routing.
 * <p>
 * The Luminol API is not a compile dependency (BentoBox also runs on servers without it), so
 * the event is bound reflectively and only registered when the class is present.
 */
public final class FoliaPortalBridge {

    private static final String EVENT_CLASS = "me.earthme.luminol.api.entity.PreEntityPortalEvent";
    private static final java.util.Set<java.util.UUID> preparing = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** A failed paste is retried by the core every tick; back off instead of pasting and logging per tick. */
    private static final java.util.Map<String, Long> pasteFailures = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long PASTE_RETRY_MILLIS = 60_000L;
    /** A stuck paste must not leave the entity's portal cancelled for the rest of the session. */
    private static final long PREPARATION_TIMEOUT_TICKS = 20L * 60;

    private FoliaPortalBridge() {
        // Utility class
    }

    /**
     * @return {@code true} if the running server has the Luminol portal event.
     */
    static boolean isSupported() {
        try {
            Class.forName(EVENT_CLASS, false, FoliaPortalBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Registers the bridge. Does nothing if the Luminol API is not on the server.
     *
     * @param plugin   BentoBox plugin
     * @param listener the portal listener that reroutes the transfer
     */
    public static void register(BentoBox plugin, PlayerTeleportListener listener) {
        if (!isSupported()) {
            return;
        }
        try {
            Class<?> eventClass = Class.forName(EVENT_CLASS, false, FoliaPortalBridge.class.getClassLoader());
            Method getEntity = eventClass.getMethod("getEntity");
            Method getPortalPos = eventClass.getMethod("getPortalPos");
            Method getDestination = eventClass.getMethod("getDestination");
            Method setDestination = findMethod(eventClass, "setDestination", World.class);
            Bukkit.getPluginManager().registerEvent(org.bukkit.event.entity.EntityPortalEnterEvent.class,
                    listener, EventPriority.NORMAL, (registered, event) -> {
                        handleEndEnter(plugin, listener, (org.bukkit.event.entity.EntityPortalEnterEvent) event);
                    }, plugin);
            EventExecutor executor = (registered, event) ->
                    reroute(plugin, listener, event, getEntity, getPortalPos, getDestination, setDestination);
            @SuppressWarnings("unchecked")
            Class<? extends Event> typedClass = (Class<? extends Event>) eventClass;
            Bukkit.getPluginManager().registerEvent(typedClass, listener, EventPriority.HIGHEST, executor, plugin);
        } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException e) {
            plugin.logError("Could not register the Luminol portal bridge: " + e.getMessage());
        }
    }

    /** Rate-limited bridge diagnostics: the portal event fires every tick while standing in a portal. */
    private static final java.util.Map<java.util.UUID, Long> BRIDGE_LOG = new java.util.concurrent.ConcurrentHashMap<>();

    private static void bridgeLog(BentoBox plugin, Entity entity, String message) {
        long now = System.currentTimeMillis();
        Long last = BRIDGE_LOG.get(entity.getUniqueId());
        if (last != null && now - last < 3000L) {
            return;
        }
        BRIDGE_LOG.put(entity.getUniqueId(), now);
        plugin.log("PortalBridge " + entity.getType() + " " + entity.getUniqueId() + ": " + message);
    }

    private static String describe(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + ","
                + location.getBlockZ();
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    static void handleEndEnter(BentoBox plugin, PlayerTeleportListener listener,
            org.bukkit.event.entity.EntityPortalEnterEvent event) {
        if (event.isCancelled() || event.getPortalType() != org.bukkit.PortalType.ENDER) return;
        if (isVanillaEndPortalEntity(event.getEntity())) return;
        World overWorld = Util.getWorld(event.getLocation().getWorld());
        if (overWorld == null || !plugin.getIWM().inWorld(overWorld)) return;
        // Own only BentoBox End portals before other world managers redirect them.
        event.setCancelled(true);
        Entity entity = event.getEntity();
        if (entity.getPortalCooldown() > 0) return;
        entity.setPortalCooldown(300);
        routeEnd(plugin, listener, entity, event.getLocation(), overWorld);
    }

    /**
     * Falling blocks have to stay on vanilla's end portal path. Sand and gravel dupers rely on the
     * server duplicating the entity, while BentoBox only moves it to the island End (or drops it
     * outright when that world is missing), which a player sees as the sand simply vanishing.
     *
     * @param entity the entity entering the portal
     * @return {@code true} when the event must be left to the server
     */
    static boolean isVanillaEndPortalEntity(Entity entity) {
        return entity instanceof org.bukkit.entity.FallingBlock;
    }

    private static void routeEnd(BentoBox plugin, PlayerTeleportListener listener, Entity entity,
            Location from, World overWorld) {
        if (!listener.isAllowedInConfig(overWorld, World.Environment.THE_END)
                || !listener.isAllowedOnServer(World.Environment.THE_END)) return;
        World target = from.getWorld().getEnvironment() == World.Environment.THE_END
                ? overWorld : plugin.getIWM().getEndWorld(overWorld);
        if (target == null) return;
        if (entity instanceof Player player) {
            listener.handleFoliaPortal(player, from, World.Environment.THE_END);
            return;
        }
        EntityPortalEvent portal = new EntityPortalEvent(entity, from, from.toVector().toLocation(target), 0);
        boolean handled = AbstractTeleportListener.dispatchPortal(portal, false);
        if (!handled && !portal.isCancelled() && portal.getTo() != null) {
            ClosestSafeSpotTeleport.builder(plugin).entity(entity).location(portal.getTo())
                    .successRunnable(() -> entity.setPortalCooldown(300)).build();
        }
    }

    /**
     * Works out which BentoBox dimension a transfer belongs to. The native destination
     * decides, except when the player leaves the end: the server sends them to the
     * overworld, but it is still an end portal trip and BentoBox has to route it into the
     * addon overworld.
     *
     * @param from        the world the portal is in
     * @param destination the world the server wants to send the entity to
     * @return the dimension the portal transfer belongs to
     */
    static World.Environment environmentOf(World from, World destination) {
        if (World.Environment.THE_END.equals(destination.getEnvironment())
                || (World.Environment.NORMAL.equals(destination.getEnvironment())
                        && World.Environment.THE_END.equals(from.getEnvironment()))) {
            return World.Environment.THE_END;
        }
        return World.Environment.NETHER;
    }

    /**
     * Routes linked island portals through native search/creation; legacy configurations
     * use BentoBox's manual island-spawn handling.
     */
    static void reroute(BentoBox plugin, PlayerTeleportListener listener, Event event,
            Method getEntity, Method getPortalPos, Method getDestination) {
        reroute(plugin, listener, event, getEntity, getPortalPos, getDestination, null);
    }

    static void reroute(BentoBox plugin, PlayerTeleportListener listener, Event event,
            Method getEntity, Method getPortalPos, Method getDestination, Method setDestination) {
        try {
            Object rawEntity = getEntity.invoke(event);
            if (!(rawEntity instanceof Entity entity)) {
                return;
            }
            Location rawPos = (Location) getPortalPos.invoke(event);
            World rawDestination = (World) getDestination.invoke(event);
            bridgeLog(plugin, entity, "event cancelled=" + ((Cancellable) event).isCancelled()
                    + " pos=" + describe(rawPos) + " destination=" + (rawDestination == null ? "null" : rawDestination.getName()));
            if (((Cancellable) event).isCancelled()) return;
            if (!preparing.isEmpty() && entity.getUniqueId() != null && preparing.contains(entity.getUniqueId())) {
                ((Cancellable) event).setCancelled(true);
                return;
            }
            Location portalPos = (Location) getPortalPos.invoke(event);
            World destination = (World) getDestination.invoke(event);
            if (portalPos == null || portalPos.getWorld() == null || destination == null) {
                return;
            }
            World overWorld = Util.getWorld(portalPos.getWorld());
            if (overWorld == null || !plugin.getIWM().inWorld(overWorld)) {
                // Not a BentoBox world, the server can pick its own destination.
                return;
            }
            World.Environment environment = environmentOf(portalPos.getWorld(), destination);
            if (!listener.isAllowedInConfig(overWorld, environment)
                    || !listener.isAllowedOnServer(environment)) {
                // Dimension is disabled in the addon config, so the transfer must not happen.
                ((Cancellable) event).setCancelled(true);
                return;
            }
            // Folia never fires EntityPortalEvent, so the entity flag has to be enforced here.
            // It defaults to false: animals, minecarts and items must not change dimension.
            if (!(entity instanceof Player) && !Flags.ENTITY_PORTAL_TELEPORT.isSetForWorld(overWorld)) {
                ((Cancellable) event).setCancelled(true);
                return;
            }
            if (environment == World.Environment.THE_END && !(entity instanceof Player)) {
                if (isVanillaEndPortalEntity(entity)) {
                    // Leave the event uncancelled so the server runs its own end portal transfer,
                    // which is the step sand and gravel dupers depend on.
                    return;
                }
                ((Cancellable) event).setCancelled(true);
                routeEnd(plugin, listener, entity, portalPos, overWorld);
                return;
            }
            if (setDestination != null && environment == World.Environment.NETHER
                    && listener.isMakePortals(overWorld, environment)
                    && listener.isIslandWorld(overWorld, environment)) {
                World target = portalPos.getWorld().getEnvironment() == World.Environment.NETHER
                        ? overWorld : plugin.getIWM().getNetherWorld(overWorld);
                if (target == null) {
                    ((Cancellable) event).setCancelled(true);
                    return;
                }
                if (entity instanceof Player player) {
                    var playerPortal = listener.prepareNativeFoliaPortal(player, portalPos,
                            portalPos.toVector().toLocation(target));
                    if (playerPortal.isCancelled() || !validTarget(playerPortal.getTo())) {
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                    applyNativeTarget(event, setDestination, playerPortal.getTo(), playerPortal.getSearchRadius(),
                            playerPortal.getCanCreatePortal() ? playerPortal.getCreationRadius() : -1);
                    prepareMissingIsland(plugin, listener, event, playerPortal, entity, overWorld);
                } else {
                    EntityPortalEvent portal = new EntityPortalEvent(entity, portalPos, portalPos.toVector().toLocation(target), 0);
                    AbstractTeleportListener.dispatchPortal(portal, true);
                    if (portal.isCancelled() || !validTarget(portal.getTo())) {
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                    applyNativeTarget(event, setDestination, portal.getTo(), portal.getSearchRadius(), 2);
                    prepareMissingIsland(plugin, listener, event, portal, entity, overWorld);
                }
                return;
            }
            // Legacy non-linking configurations retain BentoBox's safe island-spawn routing.
            // PreEntityPortalEvent has no setDestination, so island dimensions must be routed by
            // BentoBox itself; otherwise the entity is handed to the server's nether, not the island.
            if (!(entity instanceof Player) && environment == World.Environment.NETHER
                    && (setDestination != null || listener.isIslandWorld(overWorld, environment))) {
                World target = portalPos.getWorld().getEnvironment() == World.Environment.NETHER
                        ? overWorld : plugin.getIWM().getNetherWorld(overWorld);
                if (target == null) {
                    ((Cancellable) event).setCancelled(true);
                    return;
                }
                if (listener.isIslandWorld(overWorld, environment)) {
                    // Island dimensions use matching island coordinates, not vanilla's 8:1 scale.
                    // Find or build the paired portal first: the same Y in the other dimension is
                    // usually air, so a safe-spot search there finds nothing and drops the transfer.
                    ((Cancellable) event).setCancelled(true);
                    Location anchor = portalPos.toVector().toLocation(target);
                    Axis axis = AbstractTeleportListener.portalAxis(portalPos);
                    listener.ensureNetherPortal(anchor, axis, false).whenComplete((paired, error) -> {
                        if (error != null) {
                            plugin.logStacktrace(error);
                        }
                        if (paired == null) {
                            // No paired portal anywhere near the anchor. Teleporting to the raw
                            // coordinate makes the safe-spot search fall back to an arbitrary block,
                            // which is what scattered farm mobs over the island. Leave the entity in
                            // the source dimension instead.
                            return;
                        }
                        Location pairedTarget = paired;
                        // Dispatch on the entity's own region: the portal event reads the source block.
                        entity.getScheduler().execute(plugin, () -> {
                            EntityPortalEvent portal = new EntityPortalEvent(entity, portalPos, pairedTarget, 0);
                            boolean handled = AbstractTeleportListener.dispatchPortal(portal, false);
                            if (!handled && !portal.isCancelled() && portal.getTo() != null) {
                                ClosestSafeSpotTeleport.builder(plugin).entity(entity).location(portal.getTo())
                                        .portal().requirePortal()
                                        .successRunnable(() -> entity.setPortalCooldown(300)).build();
                            }
                        }, () -> { }, 1);
                    });
                    return;
                }
                setDestination.invoke(event, target);
                return;
            }
            if (!(entity instanceof Player player)) {
                return;
            }
            // Native transfer would land in the server's nether/end. Cancel it and let
            // BentoBox route the player to the addon dimension instead. Addons that use a
            // standard (shared) nether/end are handled by portalProcess as well, so this also
            // covers them.
            ((Cancellable) event).setCancelled(true);
            listener.handleFoliaPortal(player, portalPos, environment);
        } catch (Throwable e) {
            // The portal transfer is running in the region thread: never let an error escape.
            ((Cancellable) event).setCancelled(true);
            plugin.logStacktrace(e);
        }
    }

    private static boolean validTarget(Location target) {
        return target != null && target.getWorld() != null && Double.isFinite(target.getX())
                && Double.isFinite(target.getY()) && Double.isFinite(target.getZ());
    }

    private static void prepareMissingIsland(BentoBox plugin, PlayerTeleportListener listener, Event nativeEvent,
            Event portal, Entity entity, World overWorld) throws ReflectiveOperationException {
        if (!listener.isPastingMissingIslands(overWorld)) return;
        Location target = portal instanceof org.bukkit.event.player.PlayerPortalEvent player ? player.getTo()
                : ((EntityPortalEvent) portal).getTo();
        // Returning to the overworld, or a third-party redirect outside this island dimension, needs no paste.
        World nether = plugin.getIWM().getNetherWorld(overWorld);
        if (!target.getWorld().equals(nether)) return;
        var island = listener.getIsland(target);
        if (island.isEmpty()) return;
        String pasteKey = island.get().getUniqueId() + ":" + World.Environment.NETHER;
        World originWorld = entity.getWorld();
        java.util.function.Supplier<java.util.concurrent.CompletableFuture<Boolean>> preparation = () -> {
            var ready = new java.util.concurrent.CompletableFuture<Boolean>();
            if (((Cancellable) nativeEvent).isCancelled() || ((Cancellable) portal).isCancelled()
                    || !preparing.add(entity.getUniqueId())) {
                ready.complete(false);
                return ready;
            }
            Long failedAt = pasteFailures.get(pasteKey);
            if (failedAt != null && System.currentTimeMillis() - failedAt < PASTE_RETRY_MILLIS) {
                // The core retries the cancelled transfer every tick. Retrying the paste and logging
                // that fast would flood the log and the async scheduler.
                preparing.remove(entity.getUniqueId());
                ready.complete(false);
                return ready;
            }
            ready.whenComplete((value, error) -> preparing.remove(entity.getUniqueId()));
            TaskScheduler.asyncLater(plugin, () -> ready.complete(false), PREPARATION_TIMEOUT_TICKS);
            try {
                listener.hasPartnerIsland(island.get(), World.Environment.NETHER).thenCompose(exists -> {
                    if (Boolean.TRUE.equals(exists)) return java.util.concurrent.CompletableFuture.completedFuture(Boolean.TRUE);
                    // An entity cannot generate the island itself. The native transfer retries every tick,
                    // so refuse quietly instead of logging a stack trace per attempt.
                    if (!(entity instanceof Player)) return java.util.concurrent.CompletableFuture.completedFuture(Boolean.FALSE);
                    return listener.pasteOnce(island.get(), World.Environment.NETHER,
                            () -> listener.pasteMissingIsland(target.clone(), island.get(), World.Environment.NETHER))
                            .thenApply(ignored -> Boolean.TRUE);
                }).whenComplete((mayTransfer, error) -> {
                    if (error != null) {
                        Long previous = pasteFailures.put(pasteKey, System.currentTimeMillis());
                        if (previous == null || System.currentTimeMillis() - previous >= PASTE_RETRY_MILLIS) {
                            plugin.logStacktrace(error);
                            if (entity instanceof Player player) {
                                world.bentobox.bentobox.database.objects.Island current = island.get();
                                if (!current.isDeleted()) {
                                    world.bentobox.bentobox.api.user.User.getInstance(player)
                                            .sendMessage("general.errors.portal-paste-failed");
                                }
                            }
                        }
                    } else if (Boolean.TRUE.equals(mayTransfer)) {
                        pasteFailures.remove(pasteKey);
                    }
                    boolean transfer = error == null && Boolean.TRUE.equals(mayTransfer);
                    // Complete on the entity owner; native locate/create resumes once, without redispatch/retry.
                    try {
                        // Do not require the entity to stay put: the island lookup and paste take long
                        // enough for a walking player to leave the portal block, and the core drops the
                        // transfer silently when this future is false. Only a retired entity, a lost
                        // decision or a world change may abort it.
                        if (!entity.getScheduler().execute(plugin, () -> ready.complete(transfer && entity.isValid()
                                && !((Cancellable) nativeEvent).isCancelled() && !((Cancellable) portal).isCancelled()
                                && originWorld.equals(entity.getWorld()) && !island.get().isDeleted()),
                                () -> ready.complete(false), 1)) ready.complete(false);
                    } catch (Throwable schedulingError) {
                        plugin.logStacktrace(schedulingError);
                        ready.complete(false);
                    }
                });
            } catch (Throwable error) {
                plugin.logStacktrace(error);
                ready.complete(false);
            }
            return ready;
        };
        nativeEvent.getClass().getMethod("setPortalReady", java.util.function.Supplier.class)
                .invoke(nativeEvent, preparation);
    }

    private static void applyNativeTarget(Event event, Method setDestination, Location target,
            int searchRadius, int creationRadius) throws ReflectiveOperationException {
        // Fail closed on older cores: never silently lose an event's final target or portal limits.
        event.getClass().getMethod("setPortalTarget", Location.class).invoke(event, target.clone());
        event.getClass().getMethod("setSearchRadius", int.class).invoke(event, Math.max(0, searchRadius));
        event.getClass().getMethod("setCreationRadius", int.class).invoke(event, Math.max(-1, creationRadius));
        setDestination.invoke(event, target.getWorld());
    }
}
