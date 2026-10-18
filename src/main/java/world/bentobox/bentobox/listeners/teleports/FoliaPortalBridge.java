package world.bentobox.bentobox.listeners.teleports;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * Folia never fires {@link org.bukkit.event.player.PlayerPortalEvent}. Its portal code is
 * asynchronous and hands the entity straight to the server's default nether or end, so
 * BentoBox worlds would always leak into {@code minecraft:the_nether} / {@code minecraft:the_end}.
 * <p>
 * The Luminol family (Lophine) adds {@code me.earthme.luminol.api.entity.PreEntityPortalEvent},
 * which fires before the native transfer and can be cancelled. This bridge cancels that
 * transfer for BentoBox worlds and hands the player to {@link PlayerTeleportListener}, which
 * routes the player through the normal {@code PlayerPortalEvent} handling.
 * <p>
 * The Luminol API is not a compile dependency (BentoBox also runs on servers without it), so
 * the event is bound reflectively and only registered when the class is present.
 */
public final class FoliaPortalBridge {

    private static final String EVENT_CLASS = "me.earthme.luminol.api.entity.PreEntityPortalEvent";

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
            EventExecutor executor = (registered, event) ->
                    reroute(plugin, listener, event, getEntity, getPortalPos, getDestination);
            @SuppressWarnings("unchecked")
            Class<? extends Event> typedClass = (Class<? extends Event>) eventClass;
            Bukkit.getPluginManager().registerEvent(typedClass, listener, EventPriority.HIGHEST, executor, plugin);
        } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException e) {
            plugin.logError("Could not register the Luminol portal bridge: " + e.getMessage());
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
     * Keeps the native transfer out of BentoBox worlds by cancelling it and passing the
     * transfer to the listener. Non-player entities and worlds BentoBox does not manage are
     * left to the server.
     */
    static void reroute(BentoBox plugin, PlayerTeleportListener listener, Event event,
            Method getEntity, Method getPortalPos, Method getDestination) {
        try {
            if (!(getEntity.invoke(event) instanceof Player player)) {
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
            if (!listener.isAllowedInConfig(overWorld, environment)) {
                // Dimension is disabled in the addon config, so the transfer must not happen.
                ((Cancellable) event).setCancelled(true);
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
            plugin.logStacktrace(e);
        }
    }
}
