//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.teleports;


import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport;


/**
 * This class handles player teleportation between dimensions.
 *
 * @author tastybento and BONNe
 */
public class PlayerTeleportListener extends AbstractTeleportListener implements Listener
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
     * This listener checks player portal events and triggers appropriate methods to transfer
     * players to the correct location in other dimension.
     *
     * This event is triggered when player is about to being teleported because of contact with the
     * nether portal or end gateway portal (exit portal triggers respawn).
     *
     * This event is not called if nether/end is disabled in server settings.
     *
     * @param event the player portal event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortalEvent(PlayerPortalEvent event)
    {
        switch (event.getCause())
        {
        case NETHER_PORTAL -> this.portalProcess(event, World.Environment.NETHER);
        case END_PORTAL, END_GATEWAY -> this.portalProcess(event, World.Environment.THE_END);
        default -> throw new IllegalArgumentException("Unexpected value: " + event.getCause());
        }
    }


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
            Bukkit.getScheduler().runTaskLater(this.plugin, () ->
            {
                // Check again if still in portal
                if (this.inPortal.contains(uuid))
                {
                    // Create new PlayerPortalEvent
                    PlayerPortalEvent en = new PlayerPortalEvent((Player) entity,
                            event.getLocation(),
                            null,
                            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL,
                            0,
                            false,
                            0);

                    this.portalProcess(en, World.Environment.NETHER);
                }
            }, 40);
            return;
        }

        // End portals are instant transfer
        if (!Bukkit.getAllowEnd() && (type.equals(Material.END_PORTAL) || type.equals(Material.END_GATEWAY)))
        {
            // Create new PlayerPortalEvent
            PlayerPortalEvent en = new PlayerPortalEvent((Player) entity,
                    event.getLocation(),
                    null,
                    type.equals(Material.END_PORTAL) ? PlayerTeleportEvent.TeleportCause.END_PORTAL : PlayerTeleportEvent.TeleportCause.END_GATEWAY,
                            0,
                            false,
                            0);

            this.portalProcess(en, World.Environment.THE_END);
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
     * Player respawn event is triggered when player enters exit portal at the end.
     * This will take over respawn mechanism and place player on island.
     * @param event player respawn event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerExitPortal(PlayerRespawnEvent event)
    {
        if (!this.teleportOrigin.containsKey(event.getPlayer().getUniqueId()))
        {
            // Player is already processed.
            return;
        }

        World fromWorld = this.teleportOrigin.get(event.getPlayer().getUniqueId());
        World overWorld = Util.getWorld(fromWorld);

        if (overWorld == null || !this.plugin.getIWM().inWorld(overWorld))
        {
            // Not teleporting from/to bentobox worlds.
            return;
        }

        this.getIsland(overWorld, event.getPlayer()).ifPresentOrElse(island -> {
            if (!island.onIsland(event.getRespawnLocation()))
            {
                // If respawn location is outside island protection range, change location to the
                // spawn in overworld or home location.
                Location location = island.getSpawnPoint(World.Environment.NORMAL);

                if (location == null)
                {
                    // No spawn point. Rare thing. Well, use island protection center.
                    location = island.getProtectionCenter();
                }

                event.setRespawnLocation(location);
            }
        },
                () -> {
                    // Player does not an island. Try to get spawn island, and if that fails, use world spawn point.
                    // If spawn point is not safe, do nothing. Let server handle it.

                    Location spawnLocation = this.getSpawnLocation(overWorld);

                    if (spawnLocation != null)
                    {
                        event.setRespawnLocation(spawnLocation);
                    }
                });
    }



    // ---------------------------------------------------------------------
    // Section: Processors
    // ---------------------------------------------------------------------


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
        }

        if (this.inTeleport.contains(event.getPlayer().getUniqueId()))
        {
            // Player is already in teleportation.
            return;
        }

        this.inTeleport.add(event.getPlayer().getUniqueId());

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
        event.setCanCreatePortal(this.isMakePortals(overWorld, environment));
        // Default 16 is will always end up placing portal as close to X/8 coordinate as possible.
        // In most situations, 2 block value should be enough... I hope.
        event.setCreationRadius(2);

        // Set the destination location
        // If portals cannot be created, then destination is the spawn point, otherwise it's the vector
        event.setTo(this.calculateLocation(event.getFrom(), fromWorld, toWorld, environment, event.getCanCreatePortal()));

        // Find the distance from edge of island's protection and set the search radius
        this.getIsland(event.getTo()).ifPresent(island ->
        event.setSearchRadius(this.calculateSearchRadius(event.getTo(), island)));

        // Check if there is an island there or not
        if (this.isPastingMissingIslands(overWorld) &&
                this.isAllowedInConfig(overWorld, environment) &&
                this.isIslandWorld(overWorld, environment) &&
                this.getNetherEndWorld(overWorld, environment) != null &&
                this.getIsland(event.getTo()).
                filter(island -> !this.hasPartnerIsland(island, environment)).
                map(island -> {
                    event.setCancelled(true);
                    this.pasteNewIsland(event.getPlayer(), event.getTo(), island, environment);
                    return true;
                }).
                orElse(false))
        {
            // If there is no island, then processor already created island. Nothing to do more.
            return;
        }

        if (!event.isCancelled() && event.getCanCreatePortal())
        {
            // Let the server teleport
            return;
        }

        if (environment.equals(World.Environment.THE_END))
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
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!event.getPlayer().getWorld().equals(toWorld))
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
                }).
                build();
            }
        });
    }


    /**
     * Handle teleport from or to standard nether or end
     * @param event - PlayerPortalEvent
     * @param overWorld - over world
     * @param environment - environment involved
     */
    private void handleToStandardNetherOrEnd(PlayerPortalEvent event,
            World overWorld,
            World.Environment environment)
    {
        World toWorld = Objects.requireNonNull(this.getNetherEndWorld(overWorld, environment));
        Location spawnPoint = toWorld.getSpawnLocation();

        // If going to the nether and nether portals are active then just teleport to approx location
        if (environment.equals(World.Environment.NETHER) &&
                this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            spawnPoint = event.getFrom().toVector().toLocation(toWorld);
        }

        // If spawn is set as 0,63,0 in the End then move it to 100, 50 ,0.
        if (environment.equals(World.Environment.THE_END) && spawnPoint.getBlockX() == 0 && spawnPoint.getBlockZ() == 0)
        {
            // Set to the default end spawn
            spawnPoint = new Location(toWorld, 100, 50, 0);
            toWorld.setSpawnLocation(100, 50, 0);
        }

        if (this.isAllowedOnServer(environment))
        {
            // To Standard Nether or end
            event.setTo(spawnPoint);
        }
        else
        {
            // Teleport to standard nether or end
            ClosestSafeSpotTeleport.builder(this.plugin).
            entity(event.getPlayer()).
            location(spawnPoint).
            portal().
            build();
        }
    }


    /**
     * Handle teleport from or to standard nether or end (end is not possible because EXIT PORTAL triggers RESPAWN event)
     * @param event - PlayerPortalEvent
     * @param overWorld - over world
     * @param environment - environment involved
     */
    private void handleFromStandardNetherOrEnd(PlayerPortalEvent event, World overWorld, World.Environment environment)
    {
        if (environment.equals(World.Environment.NETHER) &&
                this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            // Set to location directly to the from location.
            event.setTo(event.getFrom().toVector().toLocation(overWorld));

            // Update portal search radius.
            this.getIsland(event.getTo()).ifPresent(island ->
            event.setSearchRadius(this.calculateSearchRadius(event.getTo(), island)));

            event.setCanCreatePortal(true);
            // event.setCreationRadius(16); 16 is default creation radius.
        }
        else
        {
            // Cannot be portal. Should recalculate position.
            // TODO: Currently, it is always spawn location. However, default home must be assigned.
            Location toLocation = this.getIsland(overWorld, event.getPlayer()).
                    map(island -> island.getSpawnPoint(World.Environment.NORMAL)).
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

            // Teleport to standard nether or end
            ClosestSafeSpotTeleport.builder(this.plugin).
            entity(event.getPlayer()).
            location(event.getTo()).
            portal().
            build();
        }
    }


    /**
     * Pastes the default nether or end island and teleports the player to the island's spawn point
     * @param player - player to teleport after pasting
     * @param to - the fallback location if a spawn point is not part of the blueprint
     * @param island - the island
     * @param environment - NETHER or THE_END
     */
    private void pasteNewIsland(Player player,
            Location to,
            Island island,
            World.Environment environment)
    {
        // Paste then teleport player
        this.plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon ->
        {
            // Get the default bundle's nether or end blueprint
            BlueprintBundle blueprintBundle = plugin.getBlueprintsManager().getDefaultBlueprintBundle(addon);

            if (blueprintBundle != null)
            {
                Blueprint bluePrint = this.plugin.getBlueprintsManager().getBlueprints(addon).
                        get(blueprintBundle.getBlueprint(environment));

                if (bluePrint != null)
                {
                    new BlueprintPaster(this.plugin, bluePrint, to.getWorld(), island).
                    paste().
                    thenAccept(state -> ClosestSafeSpotTeleport.builder(this.plugin).
                            entity(player).
                            location(island.getSpawnPoint(environment) == null ? to : island.getSpawnPoint(environment)).
                            portal().
                            build());
                }
                else
                {
                    this.plugin.logError("Could not paste default island in nether or end. " +
                            "Is there a nether-island or end-island blueprint?");
                }
            }
        });
    }
}
