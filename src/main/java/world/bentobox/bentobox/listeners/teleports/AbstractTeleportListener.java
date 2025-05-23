//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.teleports;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;


/**
 * This abstract class contains all common methods for entity and player teleportation.
 */
public abstract class AbstractTeleportListener
{
    /**
     * Instance of Teleportation processor.
     * @param bentoBox BentoBox plugin.
     */
    AbstractTeleportListener(@NonNull BentoBox bentoBox)
    {
        this.plugin = bentoBox;
        this.inPortal = new HashSet<>();
        this.inTeleport = new HashSet<>();
        this.teleportOrigin = new HashMap<>();
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Get island at the given location
     * @return optional island at given location
     */
    protected Optional<Island> getIsland(Location location)
    {
        return this.plugin.getIslandsManager().getProtectedIslandAt(location);
    }


    /**
     * Get island for given player at the given world.
     * @return optional island at given world.
     */
    protected Optional<Island> getIsland(World world, Player player)
    {
        return Optional.ofNullable(this.plugin.getIslandsManager().getIsland(world, player.getUniqueId()));
    }


    /**
     * Check if vanilla portals should be used
     *
     * @param world - game mode world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isMakePortals(World world, World.Environment environment)
    {
        return this.plugin.getIWM().getAddon(world).
                map(gameMode -> this.isMakePortals(gameMode, environment)).
                orElse(false);
    }


    /**
     * Check if vanilla portals should be used
     *
     * @param gameMode - game mode
     * @param environment - environment
     * @return true or false
     */
    protected boolean isMakePortals(GameModeAddon gameMode, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> gameMode.getWorldSettings().isMakeNetherPortals();
        case THE_END -> gameMode.getWorldSettings().isMakeEndPortals();
        default -> false;
        };
    }


    /**
     * Check if nether or end are generated
     *
     * @param overWorld - game world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isAllowedInConfig(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().isNetherGenerate(overWorld);
        case THE_END -> this.plugin.getIWM().isEndGenerate(overWorld);
        default -> true;
        };
    }


    /**
     * Check if the default nether or end are allowed by the server settings
     *
     * @param environment - environment
     * @return true or false
     */
    protected boolean isAllowedOnServer(World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> Bukkit.getAllowNether();
        case THE_END -> Bukkit.getAllowEnd();
        default -> true;
        };
    }


    /**
     * Check if nether or end islands are generated
     *
     * @param overWorld - over world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isIslandWorld(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().isNetherIslands(overWorld);
        case THE_END -> this.plugin.getIWM().isEndIslands(overWorld);
        default -> true;
        };
    }


    /**
     * Get the nether or end world
     *
     * @param overWorld - over world
     * @param environment - environment
     * @return nether or end world
     */
    protected World getNetherEndWorld(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().getNetherWorld(overWorld);
        case THE_END -> this.plugin.getIWM().getEndWorld(overWorld);
        default -> Util.getWorld(overWorld);
        };
    }


    /**
     * Check if the island has a nether or end island already
     *
     * @param island - island
     * @param environment - environment
     * @return true or false
     */
    protected boolean hasPartnerIsland(Island island, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> island.hasNetherIsland();
        case THE_END -> island.hasEndIsland();
        default -> true;
        };
    }


    /**
     * This method calculates the maximal search area for portal.
     * @param location Location from which search should happen.
     * @param island Island that contains the search point.
     * @return Search range for portal.
     */
    protected int calculateSearchRadius(Location location, Island island)
    {
        int diff;

        if (island.onIsland(location))
        {
            // Find max x or max z
            int x = Math.abs(island.getProtectionCenter().getBlockX() - location.getBlockX());
            int z = Math.abs(island.getProtectionCenter().getBlockZ() - location.getBlockZ());

            diff = Math.min(this.plugin.getSettings().getSafeSpotSearchRange(),
                    island.getProtectionRange() - Math.max(x, z));
        }
        else
        {
            diff = this.plugin.getSettings().getSafeSpotSearchRange();
        }

        return diff;
    }


    /**
     * This method calculates location for portal.
     * @param fromLocation Location from which teleportation happens.
     * @param fromWorld World from which teleportation happens.
     * @param toWorld The target world.
     * @param environment Portal variant.
     * @param canCreatePortal Indicates if portal should be created or not.
     * @return Location for new portal.
     */
    protected Location calculateLocation(Location fromLocation,
            World fromWorld,
            World toWorld,
            World.Environment environment,
            boolean canCreatePortal)
    {
        // Null check - not that useful
        if (fromWorld == null || toWorld == null)
        {
            return null;
        }

        Location toLocation = fromLocation.toVector().toLocation(toWorld);

        if (!this.isMakePortals(fromWorld, environment))
        {
            toLocation = Objects.requireNonNullElse(this.getIsland(fromLocation).
                    map(island -> island.getSpawnPoint(toWorld.getEnvironment())).
                    orElse(toLocation), toLocation);
        }

        // Limit Y to the min/max world height.
        toLocation.setY(Math.max(Math.min(toLocation.getY(), toWorld.getMaxHeight()), toWorld.getMinHeight()));

        if (!canCreatePortal)
        {
            // Legacy portaling
            return toLocation;
        }

        // Make portals
        // For anywhere other than the end - it is the player's location that is used
        if (!environment.equals(World.Environment.THE_END))
        {
            return toLocation;
        }

        // If the-end then we want the platform to always be generated in the same place no matter where
        // they enter the portal
        final int x = fromLocation.getBlockX();
        final int z = fromLocation.getBlockZ();
        final int y = fromLocation.getBlockY();
        int i = x;
        int j = z;
        int k = y;

        // If the from is not a portal, then we have to find it
        if (!fromLocation.getBlock().getType().equals(Material.END_PORTAL))
        {
            // Search portal block 5 blocks in all directions from starting location. Return the first one.
            boolean continueSearch = true;

            // simplistic search pattern to look at all blocks from the middle outwards by preferring
            // Y location first, then Z and as last X
            // Proper implementation would require queue and distance calculation.

            for (int offsetX = 0; continueSearch && offsetX < 10; offsetX++)
            {
                // Change sign based on mod value.
                int posX = x + ((offsetX % 2 == 0) ? 1 : -1) * (offsetX / 2);

                for (int offsetZ = 0; continueSearch && offsetZ < 10; offsetZ++)
                {
                    // Change sign based on mod value.
                    int posZ = z + ((offsetZ % 2 == 0) ? 1 : -1) * (offsetZ / 2);

                    for (int offsetY = 0; continueSearch && offsetY < 10; offsetY++)
                    {
                        // Change sign based on mod value.
                        int posY = y + ((offsetY % 2 == 0) ? 1 : -1) * (offsetY / 2);

                        if (fromWorld.getBlockAt(posX, posY, posZ).getType().equals(Material.END_PORTAL))
                        {
                            i = posX;
                            j = posZ;
                            k = posY;
                            continueSearch = false;
                        }
                    }
                }
            }
        }

        // Find the maximum x and z corner using relative block search.
        Block portalBlock = fromWorld.getBlockAt(i, k, j);

        while (portalBlock.getRelative(1, 0, 0).getType() == Material.END_PORTAL)
        {
            portalBlock = portalBlock.getRelative(1, 0, 0);
            i++;
        }

        while (portalBlock.getRelative(0, 0, 1).getType() == Material.END_PORTAL)
        {
            portalBlock = portalBlock.getRelative(0, 0, 1);
            j++;
        }

        // Mojang end platform generation is:
        // AIR
        // AIR
        // OBSIDIAN
        // and player is placed on second air block above obsidian.
        // If Y coordinate is below 2, then obsidian platform is not generated and player falls in void.
        return new Location(toWorld, i - 0.5, Math.min(toWorld.getMaxHeight() - 2, Math.max(toWorld.getMinHeight() + 2, k)), j - 0.5);
    }


    /**
     * This method returns spawn location for given world.
     * @param world World which spawn point must be returned.
     * @return Spawn location for world or null.
     */
    @Nullable
    protected Location getSpawnLocation(World world)
    {
        return this.plugin.getIslandsManager().getSpawn(world).map(island ->
        island.getSpawnPoint(World.Environment.NORMAL) == null ?
                island.getCenter() :
                    island.getSpawnPoint(World.Environment.NORMAL)).
                orElse(this.plugin.getIslands().isSafeLocation(world.getSpawnLocation()) ?
                        world.getSpawnLocation() : null);
    }


    /**
     * This method returns if missing islands should be generated upon teleportation.
     * Can happen only in non-custom generators.
     * @param overWorld OverWorld
     * @return {@code true} if missing islands must be pasted, {@code false} otherwise.
     */
    protected boolean isPastingMissingIslands(World overWorld)
    {
        return this.plugin.getIWM().isPasteMissingIslands(overWorld) &&
                !this.plugin.getIWM().isUseOwnGenerator(overWorld);
    }


    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------


    /**
     * BentoBox plugin instance.
     */
    @NonNull
    protected final BentoBox plugin;

    /**
     * Set of entities that currently is inside portal.
     */
    protected final Set<UUID> inPortal;

    /**
     * Map that links entities origin of teleportation. Used for respawning.
     */
    protected final Map<UUID, World> teleportOrigin;

    /**
     * Set of entities that currently is in teleportation.
     */
    protected final Set<UUID> inTeleport;

    /**
     * @return the inTeleport
     */
    public Set<UUID> getInTeleport() {
        return inTeleport;
    }

    /**
     * @return the inPortal
     */
    public Set<UUID> getInPortal() {
        return inPortal;
    }

    /**
     * @return the teleportOrigin
     */
    public Map<UUID, World> getTeleportOrigin() {
        return teleportOrigin;
    }
}
