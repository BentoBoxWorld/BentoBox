//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.teleports;


import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPortalExitEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport;


/**
 * This class handles entity teleportation between dimensions.
 *
 * @author BONNe
 */
public class EntityTeleportListener extends AbstractTeleportListener implements Listener
{
    /**
     * Instance of Teleportation processor.
     *
     * @param bentoBox BentoBox plugin.
     */
    public EntityTeleportListener(@NonNull BentoBox bentoBox)
    {
        super(bentoBox);
    }


    /**
     * This listener checks entity portal events and triggers appropriate methods to transfer
     * entities to the correct location in other dimension.
     * <p>
     * This event is triggered when entity is about to being teleported because of contact with the
     * nether portal or end gateway portal (exit portal triggers respawn).
     * <p>
     * This event is not called if nether/end is disabled in server settings.
     *
     * @param event the entity portal event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event)
    {
        World fromWorld = event.getFrom().getWorld();
        World overWorld = Util.getWorld(fromWorld);

        if (overWorld == null || !this.plugin.getIWM().inWorld(overWorld) || event.getTo() == null)
        {
            // Not a bentobox world.
            return;
        }

        if (!Flags.ENTITY_PORTAL_TELEPORT.isSetForWorld(overWorld))
        {
            // Teleportation is disabled. Cancel event.
            event.setCancelled(true);
            return;
        }
        // Trigger event processor.
        this.portalProcess(event, event.getTo().getWorld().getEnvironment());
    }


    /**
     * Fires the event if nether or end is disabled at the system level
     *
     * @param event - EntityPortalEnterEvent
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityEnterPortal(EntityPortalEnterEvent event)
    {
        if (EntityType.PLAYER.equals(event.getEntity().getType()))
        {
            // This handles only non-players.
            return;
        }

        Entity entity = event.getEntity();
        Material type = event.getLocation().getBlock().getType();
        UUID uuid = entity.getUniqueId();

        if (this.inPortal.contains(uuid))
        {
            // Already in process.
            return;
        }

        World fromWorld = event.getLocation().getWorld();
        World overWorld = Util.getWorld(fromWorld);

        if (overWorld == null || !this.plugin.getIWM().inWorld(overWorld))
        {
            // Not a bentobox world.
            return;
        }

        if (!Flags.ENTITY_PORTAL_TELEPORT.isSetForWorld(overWorld))
        {
            // Teleportation is disabled. Cancel processing.
            return;
        }

        this.inPortal.add(uuid);
        // Add original world for respawning.
        this.teleportOrigin.put(uuid, fromWorld);

        // Entities are teleported instantly.
        if (!Bukkit.getAllowNether() && type.equals(Material.NETHER_PORTAL))
        {
            if (fromWorld == overWorld)
            {
                this.portalProcess(
                    new EntityPortalEvent(entity, event.getLocation(), event.getLocation(), 0),
                    World.Environment.NETHER);
            }
            else
            {
                this.portalProcess(
                    new EntityPortalEvent(entity, event.getLocation(), event.getLocation(), 0),
                    World.Environment.NORMAL);
            }

            // Do not process anything else.
            return;
        }

        // Entities are teleported instantly.
        if (!Bukkit.getAllowEnd() && (type.equals(Material.END_PORTAL) || type.equals(Material.END_GATEWAY)))
        {
            if (fromWorld == this.getNetherEndWorld(overWorld, World.Environment.THE_END))
            {
                this.portalProcess(
                    new EntityPortalEvent(entity, event.getLocation(), event.getLocation(), 0),
                    World.Environment.NORMAL);
            }
            else
            {
                this.portalProcess(
                    new EntityPortalEvent(entity, event.getLocation(), event.getLocation(), 0),
                    World.Environment.THE_END);
            }
        }
    }


    /**
     * Remove inPortal flag only when entity exits the portal
     *
     * @param event entity move event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExitPortal(EntityPortalExitEvent event)
    {
        if (!this.inPortal.contains(event.getEntity().getUniqueId()))
        {
            return;
        }

        this.inPortal.remove(event.getEntity().getUniqueId());
        this.inTeleport.remove(event.getEntity().getUniqueId());
        this.teleportOrigin.remove(event.getEntity().getUniqueId());
    }


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * This method process entity teleportation to a correct dimension.
     * @param event Event that triggers teleportation.
     * @param environment Environment of the dimension where entity must appear.
     */
    private void portalProcess(EntityPortalEvent event, World.Environment environment)
    {
        World fromWorld = event.getFrom().getWorld();
        World overWorld = Util.getWorld(fromWorld);

        if (fromWorld == null || overWorld == null)
        {
            // Missing worlds.
            event.setCancelled(true);
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
            // World is disabled in bukkit. Event is not triggered, but cancel just in case.
            event.setCancelled(true);
        }
        if (this.inTeleport.contains(event.getEntity().getUniqueId()))
        {
            // Entity is already in teleportation.
            return;
        }
        this.inTeleport.add(event.getEntity().getUniqueId());

        // Get target world.
        World toWorld;

        if (environment.equals(World.Environment.NORMAL))
        {
            toWorld = overWorld;
        }
        else
        {
            toWorld = this.getNetherEndWorld(overWorld, environment);
        }

        if (!overWorld.equals(toWorld) && !this.isIslandWorld(overWorld, environment))
        {
            // This is not island world. Use standard nether or end world teleportation.
            this.handleToStandardNetherOrEnd(event, overWorld, toWorld);
            return;
        }
        
        if (!overWorld.equals(fromWorld) && !this.isIslandWorld(overWorld, environment))
        {
            // If entering a portal in the other world, teleport to a portal in overworld if
            // there is one
            this.handleFromStandardNetherOrEnd(event, overWorld, toWorld.getEnvironment());
            return;
        }
        
        // Set the destination location
        // If portals cannot be created, then destination is the spawn point, otherwise it's the vector
        event.setTo(this.calculateLocation(event.getFrom(),
            fromWorld,
            toWorld,
            environment,
            this.isMakePortals(overWorld, environment)));

        // Calculate search radius for portal
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
                    return true;
                }).
                orElse(false))
        {
            // If there is no island, then processor already entity cannot be teleported before player
            // visit that dimension.
            return;
        }
        
        if (!event.isCancelled())
        {
            // Let the server teleport
            return;
        }
        
        if (environment.equals(World.Environment.THE_END))
        {
            // Prevent death from hitting the ground while calculating location.
            event.getEntity().setVelocity(new Vector(0,0,0));
            event.getEntity().setFallDistance(0);
        }
        
        // If we do not generate portals, teleportation should happen manually with safe spot builder.
        // Otherwise, we could end up with situations when player is placed in mid air, if teleportation
        // is done instantly.
        // Our safe spot task is triggered in next tick, however, end teleportation happens in the same tick.
        // It is placed outside THE_END check, as technically it could happen with the nether portal too.

        // If there is a portal to go to already, then the player will go there
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!event.getEntity().getWorld().equals(toWorld))
            {
                // Else manually teleport entity
                ClosestSafeSpotTeleport.builder(this.plugin).
                    entity(event.getEntity()).
                    location(event.getTo()).
                    portal().
                    successRunnable(() -> {
                        // Reset velocity just in case.
                        event.getEntity().setVelocity(new Vector(0,0,0));
                        event.getEntity().setFallDistance(0);
                    }).
                    build();
            }
        });
    }


    /**
     * Handle teleport to standard nether or end
     * @param event - EntityPortalEvent
     * @param overWorld - over world
     * @param toWorld - to world
     */
    private void handleToStandardNetherOrEnd(EntityPortalEvent event, World overWorld, World toWorld)
    {
        Location spawnPoint = toWorld.getSpawnLocation();

        // If going to the nether and nether portals are active then just teleport to approx location
        if (World.Environment.NETHER.equals(toWorld.getEnvironment()) &&
            this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            spawnPoint = event.getFrom().toVector().toLocation(toWorld);
        }

        // If spawn is set as 0,63,0 in the End then move it to 100, 50 ,0.
        if (World.Environment.THE_END.equals(toWorld.getEnvironment()) && spawnPoint.getBlockX() == 0 && spawnPoint.getBlockZ() == 0)
        {
            // Set to the default end spawn
            spawnPoint = new Location(toWorld, 100, 50, 0);
            toWorld.setSpawnLocation(100, 50, 0);
        }

        if (this.isAllowedOnServer(toWorld.getEnvironment()))
        {
            // To Standard Nether or end
            event.setTo(spawnPoint);
        }
        else
        {
            // Teleport to standard nether or end
            ClosestSafeSpotTeleport.builder(this.plugin).
                entity(event.getEntity()).
                location(spawnPoint).
                portal().
                build();
        }
    }


    /**
     * Handle teleport from standard nether or end
     * @param event - EntityPortalEvent
     * @param overWorld - over world
     * @param environment - to world environment
     */
    private void handleFromStandardNetherOrEnd(EntityPortalEvent event, World overWorld, World.Environment environment)
    {
        if (World.Environment.NETHER.equals(environment) &&
            this.plugin.getIWM().getWorldSettings(overWorld).isMakeNetherPortals())
        {
            // Set to location directly to the from location.
            event.setTo(event.getFrom().toVector().toLocation(overWorld));

            // Update portal search radius.
            this.getIsland(event.getTo()).ifPresent(island ->
                event.setSearchRadius(this.calculateSearchRadius(event.getTo(), island)));
        }
        else
        {
            // Cannot be portal. Should recalculate position.
            Location spawnLocation = this.getSpawnLocation(overWorld);

            event.setTo(spawnLocation == null ?
                event.getFrom().toVector().toLocation(overWorld) :
                spawnLocation);
        }

        if (!this.isAllowedOnServer(environment))
        {
            // Custom portal handling.
            event.setCancelled(true);

            // Teleport to standard nether or end
            ClosestSafeSpotTeleport.builder(this.plugin).
                entity(event.getEntity()).
                location(event.getTo()).
                portal().
                build();
        }
    }
}
