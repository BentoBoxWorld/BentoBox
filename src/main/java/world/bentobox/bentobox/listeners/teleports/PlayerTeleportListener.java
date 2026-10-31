//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.teleports;


import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.worldsettings.IslandRespawnListener;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;


/**
 * This class handles player teleportation between dimensions.
 *
 * @author tastybento and BONNe
 */
public non-sealed class PlayerTeleportListener extends AbstractTeleportListener implements Listener
{
    /**
     * Instantiates a new Portal teleportation listener.
     *
     * @param plugin the plugin
     */
    public PlayerTeleportListener(@NonNull BentoBox plugin)
    {
        super(plugin);
    }


    // ---------------------------------------------------------------------
    // Section: Listeners
    // ---------------------------------------------------------------------

    /**
     * Fires the event if nether or end is disabled at the system level
     *
     * @param event - EntityPortalEnterEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(EntityPortalEnterEvent event)
    {
        if (!EntityType.PLAYER.equals(event.getEntity().getType()))
        {
            // This handles only players.
            return;
        }
        Entity entity = event.getEntity();
        Material type = event.getLocation().getBlock().getType();
        UUID uuid = entity.getUniqueId();

        if (this.inPortal.contains(uuid) ||
                !this.plugin.getIWM().inWorld(Util.getWorld(event.getLocation().getWorld())))
        {
            return;
        }
        this.inPortal.add(uuid);
        // Add original world for respawning.
        this.teleportOrigin.put(uuid, event.getLocation().getWorld());

        if (!Bukkit.getAllowNether() && type.equals(Material.NETHER_PORTAL))
        {
            // Schedule a time
            TaskScheduler.entityLater(this.plugin, entity, () ->
            {
                // Check again if still in portal
                if (this.inPortal.contains(uuid))
                {
                    // Create new PlayerPortalEvent and post it through the event bus so that
                    // protection flag listeners (e.g. PortalListener) can inspect and cancel it
                    // before BentoBox processes the actual teleportation.
                    PlayerPortalEvent en = new PlayerPortalEvent((Player) entity,
                            event.getLocation(),
                            null,
                            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
                            0,
                            false,
                            0);

                    dispatchPortal(en, false);
                }
            }, 40);
            return;
        }
        // End portals are instant transfer
        if (!Bukkit.getAllowEnd() && (type.equals(Material.END_PORTAL) || type.equals(Material.END_GATEWAY)))
        {
            // Create new PlayerPortalEvent and post it through the event bus so that
            // protection flag listeners (e.g. PortalListener) can inspect and cancel it
            // before BentoBox processes the actual teleportation.
            PlayerPortalEvent en = new PlayerPortalEvent((Player) entity,
                    event.getLocation(),
                    null,
                    type.equals(Material.END_PORTAL) ? PlayerTeleportEvent.TeleportCause.END_PORTAL : PlayerTeleportEvent.TeleportCause.END_GATEWAY,
                            0,
                            false,
                            0);

            dispatchPortal(en, false);
        }
    }


    /**
     * Remove inPortal flag only when player exits the portal
     *
     * @param event player move event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExitPortal(PlayerMoveEvent event)
    {
        if (!this.inPortal.contains(event.getPlayer().getUniqueId()))
        {
            return;
        }

        if (event.getTo() != null && !event.getTo().getBlock().getType().equals(Material.NETHER_PORTAL))
        {
            // Player exits nether portal.
            this.inPortal.remove(event.getPlayer().getUniqueId());
            this.inTeleport.remove(event.getPlayer().getUniqueId());
            this.teleportOrigin.remove(event.getPlayer().getUniqueId());
        }
    }


    /**
     * Player respawn event is triggered when player enters the exit portal in the end.
     * This will take over the respawn mechanism and place the player back on their island
     * instead of the server's default spawn. Valid bed or respawn anchor spawns on an
     * island the player is a member of are honored if the
     * {@link world.bentobox.bentobox.lists.Flags#BED_ANCHOR_RESPAWN} world setting allows it.
     * @param event player respawn event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerExitPortal(PlayerRespawnEvent event)
    {
        if (!PlayerRespawnEvent.RespawnReason.END_PORTAL.equals(event.getRespawnReason()))
        {
            // Only handle respawns caused by entering the end exit portal.
            return;
        }

        // The player is still in the end world when this event fires.
        World overWorld = Util.getWorld(event.getPlayer().getWorld());

        if (overWorld == null || !this.plugin.getIWM().inWorld(overWorld))
        {
            // Not exiting the end of a bentobox world.
            return;
        }

        // No longer needed for routing; clean up so a stale entry cannot linger.
        this.teleportOrigin.remove(event.getPlayer().getUniqueId());

        if (IslandRespawnListener.keepVanillaSpawn(this.plugin, event, overWorld))
        {
            // Vanilla resolved a bed/anchor spawn on an island the player is a member of.
            return;
        }

        this.getIsland(overWorld, event.getPlayer()).ifPresentOrElse(island -> {
            // Route to the player's home, the same way ISLAND_RESPAWN does after death.
            Location respawnLocation =
                    this.plugin.getIslands().getSafeRespawnLocation(overWorld, event.getPlayer().getUniqueId());

            if (respawnLocation != null)
            {
                event.setRespawnLocation(respawnLocation);
            }
            else
            {
                // Anchor the respawn at the island center so the player does not appear at
                // the server spawn, then schedule SafeSpotTeleport to find a truly safe spot.
                event.setRespawnLocation(island.getProtectionCenter());
                TaskScheduler.entity(this.plugin, event.getPlayer(), () ->
                        new SafeSpotTeleport.Builder(this.plugin)
                                .entity(event.getPlayer())
                                .island(island)
                                .cancelIfFail(true)
                                .build());
            }
        },
                () -> {
                    // Player does not have an island. Try to get spawn island, and if that fails, use world spawn point.
                    // If spawn point is not safe, do nothing. Let server handle it.

                    Location spawnLocation = this.getSpawnLocation(overWorld);

                    if (spawnLocation != null)
                    {
                        event.setRespawnLocation(spawnLocation);
                    }
                });
    }

    /**
     * This listener checks player portal events and triggers appropriate methods to transfer
     * players to the correct location in other dimension.
     * <p>
     * This event is triggered when player is about to being teleported because of contact with the
     * nether portal or end gateway portal (exit portal triggers respawn).
     * <p>
     * This event is not called if nether/end is disabled in server settings.
     *
     * @param event the player portal event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        if (TaskScheduler.isFolia() && event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
                && event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END) {
            World overWorld = Util.getWorld(event.getFrom().getWorld());
            if (overWorld != null && plugin.getIWM().inWorld(overWorld)) {
                if (!isAllowedInConfig(overWorld, World.Environment.THE_END)
                        || !isAllowedOnServer(World.Environment.THE_END)) {
                    event.setCancelled(true);
                    return;
                }
                handleFromStandardNetherOrEnd(event, overWorld, World.Environment.THE_END);
                return;
            }
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && TaskScheduler.isFolia()
                && event.getFrom().getWorld() != null
                && event.getFrom().getWorld().getEnvironment() == World.Environment.NORMAL) {
            World overWorld = Util.getWorld(event.getFrom().getWorld());
            World netherWorld = overWorld == null ? null : this.plugin.getIWM().getNetherWorld(overWorld);
            if (netherWorld != null && overWorld != null && this.plugin.getIWM().inWorld(overWorld)
                    && this.isAllowedInConfig(overWorld, World.Environment.NETHER)
                    && this.isAllowedOnServer(World.Environment.NETHER)
                    && !this.isIslandWorld(overWorld, World.Environment.NETHER)) {
                event.setTo(this.calculateLocation(event.getFrom(), event.getFrom().getWorld(), netherWorld,
                        World.Environment.NETHER, event.getCanCreatePortal()));
                // Folia's native transfer is cancelled by FoliaPortalBridge, so the teleport has
                // to be done here instead of leaving it to the server.
                this.teleportToStandardNetherOrEnd(event);
                return;
            }
        }
        switch (event.getCause()) {
        case NETHER_PORTAL -> this.portalProcess(event, World.Environment.NETHER);
        case END_PORTAL -> this.portalProcess(event, World.Environment.THE_END);
        // End gateways move the player within the End (outer islands). Let the server resolve
        // them: treating THE_END -> THE_END as an exit would send the player to the overworld.
        case END_GATEWAY -> { // Do nothing, server handles the gateway
        }
        default -> { // Do nothing, ignore

        }
        /*
         * Other potential reasons: CHORUS_FRUIT , COMMAND, DISMOUNT,
         * ENDER_PEARL, EXIT_BED, PLUGIN, SPECTATE , UNKNOWN
        */
        }
    }

    /**
     * Handles a portal transfer that the server will not deliver as a PlayerPortalEvent.
     * Folia resolves portals asynchronously and never fires PlayerPortalEvent, so
     * {@link FoliaPortalBridge} cancels the native transfer and calls this method. The event
     * is posted on the normal event bus so protection flag listeners (e.g. PortalListener)
     * can inspect and cancel it before BentoBox teleports the player.
     *
     * @param player      the player using the portal
     * @param portalPos   the portal block the player entered
     * @param environment the dimension the player is sent to
     */
    PlayerPortalEvent prepareNativeFoliaPortal(Player player, Location portalPos, Location destination) {
        PlayerPortalEvent event = new PlayerPortalEvent(player, portalPos, destination,
                PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
        dispatchPortal(event, true);
        return event;
    }

    void handleFoliaPortal(Player player, Location portalPos, World.Environment environment) {
        PlayerPortalEvent event = new PlayerPortalEvent(player,
                portalPos,
                null,
                World.Environment.THE_END.equals(environment) ?
                        PlayerTeleportEvent.TeleportCause.END_PORTAL :
                            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
                0,
                false,
                0);

        dispatchPortal(event, false);
    }

    // ---------------------------------------------------------------------
    // Section: Processors
    // ---------------------------------------------------------------------

    /** Rate-limited diagnostics for the portal path; see {@link #logPortal(Player, String)}. */
    private static final Map<UUID, Long> PORTAL_LOG = new ConcurrentHashMap<>();

    /**
     * Logs a portal decision at most once every five seconds per player. The transfer has several
     * silent bail-outs, so without this a portal that does nothing leaves no trace at all.
     */
    private void logPortal(Player player, String message) {
        long now = System.currentTimeMillis();
        Long last = PORTAL_LOG.get(player.getUniqueId());
        if (last != null && now - last < 5000L) {
            return;
        }
        PORTAL_LOG.put(player.getUniqueId(), now);
        plugin.log("Portal " + player.getName() + ": " + message);
    }


    /**
     * This method process player teleportation to new dimension.
     * @param event Event that triggers teleportation.
     * @param environment Environment of portal type.
     */
    private void portalProcess(PlayerPortalEvent event, World.Environment environment)
    {
        World fromWorld = event.getFrom().getWorld();
        World overWorld = Util.getWorld(fromWorld);

        if (overWorld == null || !this.plugin.getIWM().inWorld(overWorld))
        {
            // Not teleporting from/to bentobox worlds.
            return;
        }
        if (!this.isAllowedInConfig(overWorld, environment))
        {
            // World is disabled in config. Do not teleport player.
            event.setCancelled(true);
            return;
        }
        if (!this.isAllowedOnServer(environment))
        {
            // World is disabled in bukkit. Event is not triggered, but cancel by chance.
            event.setCancelled(true);
            return;
        }
        if (this.inTeleport.contains(event.getPlayer().getUniqueId()))
        {
            // Player is already in teleportation.
            logPortal(event.getPlayer(), "ignored: a transfer is already in flight");
            return;
        }

        if (fromWorld.equals(overWorld) && !this.isIslandWorld(overWorld, environment))
        {
            // This is not island world. Use standard nether or end world teleportation.
            this.handleToStandardNetherOrEnd(event, overWorld, environment);
            return;
        }
        if (!fromWorld.equals(overWorld) && !this.isIslandWorld(overWorld, environment))
        {
            // If entering a portal in the other world, teleport to a portal in overworld if
            // there is one
            this.handleFromStandardNetherOrEnd(event, overWorld, environment);
            return;
        }
        // To the nether/end or overworld.
        World toWorld = !fromWorld.getEnvironment().equals(environment) ?
                this.getNetherEndWorld(overWorld, environment) : overWorld;

        // Set whether portals should be created or not
        event.setCanCreatePortal(this.isMakePortals(overWorld, environment));        // Blueprint spawns already have terrain; regenerating a platform would overwrite the island.
        if (getEndIslandSpawn(event.getFrom(), toWorld).isPresent()) event.setCanCreatePortal(false);
        // Default 16 is will always end up placing portal as close to X/8 coordinate as possible.
        // In most situations, 2 block value should be enough... I hope.
        event.setCreationRadius(2);

        // Set the destination location
        // If portals cannot be created, then destination is the spawn point, otherwise it's the vector
        Location destination = this.calculateLocation(event.getFrom(), fromWorld, toWorld, environment,
                event.getCanCreatePortal());
        if (destination == null) {
            // Destination world is not loaded: mirror EntityTeleportListener and leave the player be
            logPortal(event.getPlayer(), "no destination: from=" + fromWorld.getName()
                    + " to=" + toWorld.getName() + " environment=" + environment);
            event.setCancelled(true);
            return;
        }
        event.setTo(destination);
        // One line per portal entry: without it we cannot tell whether the destination or the
        // portal search is wrong when the transfer fails.
        plugin.log("Portal " + event.getPlayer().getName() + ": destination " + toWorld.getName() + " "
                + destination.getBlockX() + "," + destination.getBlockY() + "," + destination.getBlockZ()
                + " canCreate=" + event.getCanCreatePortal()
                + " makePortals=" + this.isMakePortals(overWorld, environment));

        // Find the distance from edge of island's protection and set the search radius
        this.getIsland(event.getTo()).ifPresent(island ->
            event.setSearchRadius(this.calculateSearchRadius(event.getTo(), island)));

        boolean synthetic = isSyntheticPortal();
        // Real Bukkit events retain the server's continuation; never launch a second async transfer.
        if (!synthetic && !TaskScheduler.isFolia()) {
            // A missing dimension island must be pasted before the native transfer, so take the
            // trip over: cancel the event and teleport once the blueprint is in place.
            if (this.tryPasteMissingIsland(event, overWorld, environment)) return;
            if (environment != World.Environment.THE_END) return;
            afterPortal(event, event.getPlayer(), () -> {
                if (event.getCanCreatePortal() && event.getTo() != null && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END
                        && getEndIslandSpawn(event.getFrom(), event.getTo().getWorld()).isEmpty()) createEndPlatform(event.getTo());
            });
            return;
        }
        afterPortal(event, event.getPlayer(), () -> finishPortal(event, overWorld, environment));
    }

    private void finishPortal(PlayerPortalEvent event, World overWorld, World.Environment environment) {
        if (event.isCancelled() || event.getTo() == null) {
            logPortal(event.getPlayer(), "abandoned before teleport: cancelled=" + event.isCancelled()
                    + " to=" + event.getTo() + " environment=" + environment);
            return;
        }
        this.inTeleport.add(event.getPlayer().getUniqueId());
        // Check if there is an island there or not
        Island island = this.getIsland(event.getTo()).orElse(null);
        if (island != null && this.isPastingMissingIslands(overWorld) &&
                this.isAllowedInConfig(overWorld, environment) &&
                this.isIslandWorld(overWorld, environment) &&
                this.getNetherEndWorld(overWorld, environment) != null)
        {
            Location destination = event.getTo().clone();
            hasPartnerIsland(island, environment).thenAccept(exists ->
                    TaskScheduler.entity(plugin, event.getPlayer(), () -> {
                        if (event.isCancelled()) return;
                        if (!exists) {
                            pasteNewIsland(event.getPlayer(), destination, island, environment);
                        } else if (World.Environment.NETHER == environment && this.isMakePortals(overWorld, environment)) {
                            // Vanilla reuses or creates the paired portal and puts the player in
                            // it, so follow the same order before handing over to the teleport.
                            this.ensureNetherPortal(destination, portalAxis(event.getFrom()), true)
                                    .whenComplete((portal, error) -> {
                                        if (error != null) {
                                            plugin.logStacktrace(error);
                                        } else if (portal != null && !event.isCancelled()) {
                                            event.setTo(portal);
                                        } else if (portal == null) {
                                            logPortal(event.getPlayer(), "no portal could be found or built near "
                                                    + destination.getBlockX() + "," + destination.getBlockY()
                                                    + "," + destination.getBlockZ() + " in "
                                                    + destination.getWorld().getName());
                                        }
                                        completePortal(event);
                                    });
                        } else {
                            completePortal(event);
                        }
                    })).exceptionally(error -> { plugin.logStacktrace(error); return null; });
            return;
        }
        // Even when missing islands are not pasted, the trip must leave a portal behind or the
        // player has no way back. Reuse an existing portal near the destination, else build one.
        if (World.Environment.NETHER == environment && this.isMakePortals(overWorld, environment))
        {
            Location destination = event.getTo().clone();
            this.ensureNetherPortal(destination, portalAxis(event.getFrom()), true)
                    .whenComplete((portal, error) -> {
                        if (error != null) {
                            plugin.logStacktrace(error);
                        } else if (portal != null && !event.isCancelled()) {
                            event.setTo(portal);
                        }
                        completePortal(event);
                    });
            return;
        }
        completePortal(event);
    }

    private void completePortal(PlayerPortalEvent event) {
        if (event.isCancelled() || event.getTo() == null) return;
        if (TaskScheduler.isFolia() && !event.isCancelled() && event.getCanCreatePortal()
                && getEndIslandSpawn(event.getFrom(), event.getTo().getWorld()).isEmpty()
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            Location destination = event.getTo().clone();
            this.createEndPlatformAsync(destination)
                    .thenCompose(ignored -> Util.teleportAsync(event.getPlayer(), destination))
                    .exceptionally(error -> { plugin.logStacktrace(error); return false; });
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY)
        {
            // Prevent death from hitting the ground while calculating location.
            event.getPlayer().setVelocity(new Vector(0,0,0));
            event.getPlayer().setFallDistance(0);
        }

        // If we do not generate portals, teleportation should happen manually with safe spot builder.
        // Otherwise, we could end up with situations when player is placed in mid air, if teleportation
        // is done instantly.
        // Our safe spot task is triggered in next tick, however, end teleportation happens in the same tick.
        // It is placed outside THE_END check, as technically it could happen with the nether portal too.

        // If there is a portal to go to already, then the player will go there
        TaskScheduler.entity(this.plugin, event.getPlayer(), () -> {
            if (!event.isCancelled() && event.getTo() != null
                    && !event.getPlayer().getWorld().equals(event.getTo().getWorld()))
            {
                // Else manually teleport entity
                ClosestSafeSpotTeleport.builder(this.plugin).
                entity(event.getPlayer()).
                location(event.getTo()).
                portal().
                successRunnable(() -> {
                    // Reset velocity just in case.
                    event.getPlayer().setVelocity(new Vector(0,0,0));
                    event.getPlayer().setFallDistance(0);
                    // Vanilla cooldown, otherwise arriving inside the portal bounces the player back.
                    event.getPlayer().setPortalCooldown(300);
                    logPortal(event.getPlayer(), "teleported to " + event.getTo().getWorld().getName() + " "
                            + event.getTo().getBlockX() + "," + event.getTo().getBlockY() + ","
                            + event.getTo().getBlockZ());
                }).
                build();
            } else {
                logPortal(event.getPlayer(), "not teleporting: cancelled=" + event.isCancelled()
                        + " to=" + event.getTo()
                        + " from=" + event.getPlayer().getWorld().getName()
                        + (event.getTo() == null ? "" : " to=" + event.getTo().getWorld().getName()));
            }
        });
    }


    /**
     * Handle teleport from or to standard nether or end
     * @param event - PlayerPortalEvent
     * @param overWorld - over world
     * @param environment - environment involved
     */
    void handleToStandardNetherOrEnd(PlayerPortalEvent event,
            World overWorld,
            World.Environment environment)
    {
        World toWorld = Objects.requireNonNull(this.getNetherEndWorld(overWorld, environment));
        Location spawnPoint = toWorld.getSpawnLocation();

        // If going to the nether and nether portals are active then just teleport to approx location
        if (World.Environment.NETHER.equals(environment) &&
                this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            spawnPoint = event.getFrom().toVector().toLocation(toWorld);
        }

        // If spawn is set as 0,63,0 in the End then move it to 100, 50 ,0.
        if (World.Environment.THE_END.equals(environment) && spawnPoint.getBlockX() == 0 && spawnPoint.getBlockZ() == 0)
        {
            // Set to the default end spawn
            spawnPoint = new Location(toWorld, 100, 50, 0);
            toWorld.setSpawnLocation(100, 50, 0);
        }

        if (this.isAllowedOnServer(environment))
        {
            // To Standard Nether or end
            event.setTo(spawnPoint);
            this.teleportToStandardNetherOrEnd(event);
        }
        else event.setCancelled(true);
    }


    /**
     * Finishes a standard nether/end portal transfer on Folia. The server honours
     * {@link PlayerPortalEvent#setTo(Location)} itself on Paper, but on Folia
     * {@link FoliaPortalBridge} cancelled the native transfer and nothing else will do it.
     * @param event portal event with the destination set
     */
    private void teleportToStandardNetherOrEnd(PlayerPortalEvent event)
    {
        if (!TaskScheduler.isFolia() || event.getTo() == null)
        {
            return;
        }
        afterPortal(event, event.getPlayer(), () -> {
            if (event.isCancelled() || event.getTo() == null) return;
                ClosestSafeSpotTeleport.builder(this.plugin).
                entity(event.getPlayer()).
                location(event.getTo().clone()).
                portal().
                successRunnable(() -> event.getPlayer().setPortalCooldown(300)).
                build();
        });
    }



    /**
     * Handle teleport from or to standard nether or end (end is not possible because EXIT PORTAL triggers RESPAWN event)
     * @param event - PlayerPortalEvent
     * @param overWorld - over world
     * @param environment - environment involved
     */
    private void handleFromStandardNetherOrEnd(PlayerPortalEvent event, World overWorld, World.Environment environment)
    {
        if (World.Environment.NETHER.equals(environment) &&
                this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            // Set to location directly to the from location.
            event.setTo(event.getFrom().toVector().toLocation(overWorld));

            // Update portal search radius.
            this.getIsland(event.getTo()).ifPresent(island ->
            event.setSearchRadius(this.calculateSearchRadius(event.getTo(), island)));

            event.setCanCreatePortal(true);

        }
        else
        {
            // Cannot be portal. Should recalculate position.
            // Prefer the island's spawn point, but most islands never get one because it is only
            // set by a {spawn here} sign in the blueprint. Fall back to the island's home so that
            // players returning from a standard nether always land on their own island instead of
            // the world spawn, which is usually somebody else's island near 0,0.
            Location toLocation = this.getIsland(overWorld, event.getPlayer()).
                    map(island -> Objects.requireNonNullElseGet(island.getSpawnPoint(World.Environment.NORMAL),
                            () -> this.plugin.getIslands().getHomeLocation(island))).
                    orElseGet(() -> {
                        // If player do not have island, try spawn.
                        Location spawnLocation = this.getSpawnLocation(overWorld);
                        return spawnLocation == null ?
                                event.getFrom().toVector().toLocation(overWorld) :
                                    spawnLocation;
                    });

            event.setTo(toLocation);
        }

        if (!this.isAllowedOnServer(environment))
        {
            // Custom portal handling.
            event.setCancelled(true);

        }
        else
        {
            // Folia cannot use the event destination, so BentoBox teleports the player itself.
            this.teleportToStandardNetherOrEnd(event);
        }
    }


    /**
     * Pastes the default nether or end island and teleports the player to the island's spawn point
     * @param player - player to teleport after pasting
     * @param to - the fallback location if a spawn point is not part of the blueprint
     * @param island - the island
     * @param environment - NETHER or THE_END
     */
    java.util.concurrent.CompletableFuture<Void> pasteMissingIsland(Location to, Island island,
            World.Environment environment) {
        var addon = plugin.getIWM().getAddon(island.getWorld()).orElseThrow();
        BlueprintBundle bundle = plugin.getBlueprintsManager().getDefaultBlueprintBundle(addon);
        Blueprint blueprint = bundle == null ? null : plugin.getBlueprintsManager().getBlueprints(addon)
                .get(bundle.getBlueprint(environment));
        if (blueprint == null) return java.util.concurrent.CompletableFuture.failedFuture(
                new IllegalStateException("Missing default " + environment + " island blueprint"));
        return new BlueprintPaster(plugin, blueprint, to.getWorld(), island).paste().thenApply(state -> null);
    }

    /**
     * Pastes a missing dimension island before the server's own transfer would land the player
     * in an ungenerated dimension. Mirrors the Folia/synthetic path for real Bukkit events.
     *
     * @return true if this method took over the transfer
     */
    private boolean tryPasteMissingIsland(PlayerPortalEvent event, World overWorld, World.Environment environment) {
        if (event.getTo() == null || !this.isPastingMissingIslands(overWorld)
                || !this.isAllowedInConfig(overWorld, environment)
                || !this.isIslandWorld(overWorld, environment)
                || this.getNetherEndWorld(overWorld, environment) == null) return false;
        Island island = this.getIsland(event.getTo()).orElse(null);
        if (island == null || this.hasPartnerIslandSync(island, environment)) return false;
        event.setCancelled(true);
        this.inTeleport.add(event.getPlayer().getUniqueId());
        this.pasteNewIsland(event.getPlayer(), event.getTo().clone(), island, environment);
        return true;
    }

    private void pasteNewIsland(Player player,
            Location to,
            Island island,
            World.Environment environment)
    {
        // Paste then teleport player
        var addon = this.plugin.getIWM().getAddon(island.getWorld()).orElse(null);
        // Get the default bundle's nether or end blueprint
        BlueprintBundle blueprintBundle = addon == null ? null
                : plugin.getBlueprintsManager().getDefaultBlueprintBundle(addon);
        Blueprint bluePrint = blueprintBundle == null ? null
                : this.plugin.getBlueprintsManager().getBlueprints(addon).get(blueprintBundle.getBlueprint(environment));
        if (bluePrint == null) {
            // Nothing will be pasted, so release the player instead of cancelling silently.
            this.plugin.logError("Could not paste default island in nether or end. " +
                    "Is there a nether-island or end-island blueprint?");
            this.inTeleport.remove(player.getUniqueId());
            User.getInstance(player).sendMessage("general.errors.portal-paste-failed");
            return;
        }
        // One paste per island dimension: a second member entering at the same time
        // waits for this paste instead of pasting the same island again.
        Location spawn = island.getSpawnPoint(environment) == null ? to : island.getSpawnPoint(environment);
        this.pasteOnce(island, environment, () -> new BlueprintPaster(this.plugin, bluePrint,
                to.getWorld(), island).paste().thenApply(state -> null)).
        thenCompose(ignored -> World.Environment.NETHER == environment
                ? this.ensureNetherPortal(spawn, Axis.X)
                : java.util.concurrent.CompletableFuture.<Location>completedFuture(null)).
        thenAccept(portal -> ClosestSafeSpotTeleport.builder(this.plugin).
                entity(player).
                location(portal == null ? spawn : portal).
                portal().
                successRunnable(() -> player.setPortalCooldown(300)).
                build()).
        exceptionally(error -> {
            // A failed paste used to leave the player in the portal with the
            // in-teleport guard set and no island: clear it and say why.
            this.plugin.logStacktrace(error);
            this.inTeleport.remove(player.getUniqueId());
            User.getInstance(player).sendMessage("general.errors.portal-paste-failed");
            return null;
        });
    }
}
