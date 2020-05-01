package world.bentobox.bentobox.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Handles teleportation via the Nether/End portals to the Nether and End dimensions of the worlds added by the GameModeAddons.
 *
 * @author tastybento
 */
public class PortalTeleportationListener implements Listener {

    private final BentoBox plugin;

    public PortalTeleportationListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles non-player portal use.
     * Dropped items cannot teleport
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onEntityNetherPortal(EntityPortalEvent e) {
        if (e.getFrom().getWorld() == null
                || !plugin.getIWM().inWorld(e.getFrom().getWorld())) {
            return false;
        }
        /*
        // Check flag
        if (!Flags.ENTITY_TELEPORT.isSetForWorld(Util.getWorld(e.getFrom().getWorld()))) {
            e.setCancelled(true);
            return false;
        }
        if (plugin.getIWM().inWorld(e.getFrom()) && !e.getEntityType().equals(EntityType.DROPPED_ITEM)) {
            plugin.logDebug("In world and not a dropped item");
            plugin.logDebug("Flag " + Flags.ENTITY_TELEPORT.isSetForWorld(Util.getWorld(e.getFrom().getWorld())));
            // Disable dropped item due to dupe glitching
            e.setCancelled(!Flags.ENTITY_TELEPORT.isSetForWorld(Util.getWorld(e.getFrom().getWorld())));
        }*/
        Location from = e.getFrom();
        World overWorld = Util.getWorld(from.getWorld());

        // Standard nether check
        // STANDARD Environment
        if (plugin.getIWM().isNetherGenerate(overWorld) && !plugin.getIWM().isNetherIslands(overWorld)) {
            if (from.getWorld().getEnvironment() != Environment.NETHER) {
                // To nether
                e.setTo(plugin.getIWM().getNetherWorld(overWorld).getSpawnLocation());
                return true;
            }
            return false;
        }

        // We will handle this event
        e.setCancelled(true);

        return portalCheck(from, overWorld, e.getEntity(),
                plugin.getIWM().isNetherGenerate(overWorld),
                plugin.getIWM().getNetherWorld(overWorld));


    }

    /**
     * Handles end portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public boolean onEndIslandPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.END_PORTAL || e.getFrom().getWorld() == null
                || !plugin.getIWM().inWorld(e.getFrom().getWorld())) {
            return false;
        }
        Location from = e.getFrom();
        World overWorld = Util.getWorld(from.getWorld());

        // Standard end check
        if (plugin.getIWM().isEndGenerate(overWorld) && !plugin.getIWM().isEndIslands(overWorld)) {
            Location to = from.getWorld().getEnvironment() != Environment.THE_END ? plugin.getIWM().getNetherWorld(overWorld).getSpawnLocation() : null;
            return standardTeleport(e, from.getWorld(), overWorld, to);
        }

        // We will handle this event
        e.setCancelled(true);

        return portalCheck(from, overWorld, e.getPlayer(),
                plugin.getIWM().isEndGenerate(overWorld),
                plugin.getIWM().getEndWorld(overWorld));
    }

    /**
     * Handles nether portals.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) // Use HIGH to allow Multiverse first shot
    public boolean onNetherPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.NETHER_PORTAL || e.getFrom().getWorld() == null
                || !plugin.getIWM().inWorld(e.getFrom().getWorld())) {
            return false;
        }
        Location from = e.getFrom();
        World overWorld = Util.getWorld(from.getWorld());

        // Standard nether check
        // STANDARD Environment
        if (plugin.getIWM().isNetherGenerate(overWorld) && !plugin.getIWM().isNetherIslands(overWorld)) {
            Location to = from.getWorld().getEnvironment() != Environment.NETHER ? plugin.getIWM().getNetherWorld(overWorld).getSpawnLocation() : null;
            return standardTeleport(e, from.getWorld(), overWorld, to);
        }
        // We will handle this event
        e.setCancelled(true);

        return portalCheck(from, overWorld, e.getPlayer(),
                plugin.getIWM().isNetherGenerate(overWorld),
                plugin.getIWM().getNetherWorld(overWorld));
    }

    private boolean portalCheck(Location from, World overWorld, @NonNull Entity entity, boolean generated, @Nullable World toWorld) {
        // If there is no nether or end, then do not teleport.
        if (!generated) {
            return false;
        }
        Environment env = toWorld.getEnvironment();

        // FROM nether or end
        // If entering a portal in the nether or end, teleport to portal in overworld if there is one
        if (from.getWorld().getEnvironment() == env) {
            teleportToOverworld(overWorld, from, entity);
        } else {
            // TO NETHER OR END
            teleportToIslandEnv(overWorld, from, entity, env, toWorld);
        }
        return true;
    }

    private boolean standardTeleport(PlayerPortalEvent e, World fromWorld, World overWorld, Location to) {
        if (to != null) {
            // To Standard environment
            e.setTo(to);
            return true;
        } else {
            if (plugin.getIslands().hasIsland(overWorld, e.getPlayer().getUniqueId())
                    || plugin.getIslands().inTeam(overWorld, e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                plugin.getIslands().homeTeleport(overWorld, e.getPlayer());
                return true;
            }
            // No island, so just do nothing
            return false;
        }
    }

    /**
     * Teleport to an island environment
     * @param overWorld - overworld world
     * @param from - location
     * @param entity - entity teleporting
     * @param env - environment involved NETHER or THE_END
     * @param toWorld - to world
     */
    private void teleportToIslandEnv(World overWorld, @NonNull Location from, @NonNull Entity entity, Environment env, @Nullable World toWorld) {
        if (toWorld == null) return;
        // If this is to island nether, then go to the same vector, otherwise try spawn
        Optional<Island> optionalIsland = plugin.getIslands().getIslandAt(from);
        Location to = optionalIsland.map(i -> i.getSpawnPoint(env)).orElse(from.toVector().toLocation(toWorld));
        // Check if there is an island there or not
        if (entity instanceof Player &&
                plugin.getIWM().isPasteMissingIslands(overWorld) &&
                !plugin.getIWM().isUseOwnGenerator(overWorld)
                //&& plugin.getIWM().isNetherGenerate(overWorld)
                //&& plugin.getIWM().isNetherIslands(overWorld)
                && toWorld != null
                && optionalIsland.filter(i -> (env.equals(Environment.NETHER) && !i.hasNetherIsland())
                        || (env.equals(Environment.THE_END) && !i.hasEndIsland())).map(i -> {
                            // No island present so paste the default one
                            pasteNewIsland((Player)entity, to, i, env);
                            return true;
                        }).orElse(false)) {
            // All done here
            return;
        }
        // Else other worlds teleport to the
        new SafeSpotTeleport.Builder(plugin)
        .entity(entity)
        .location(to)
        .portal()
        .build();

    }

    private void teleportToOverworld(World overWorld, @NonNull Location from, @NonNull Entity entity) {
        // If this is from the island env, then go to the same vector, otherwise try island home location
        Location to = plugin.getIslands().getIslandAt(from).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(from.toVector().toLocation(overWorld));
        // Else other worlds teleport to the nether
        new SafeSpotTeleport.Builder(plugin)
        .entity(entity)
        .location(to)
        .portal()
        .build();

    }


    /**
     * Pastes the default nether or end island and teleports the player to the island's spawn point
     * @param player - player to teleport after pasting
     * @param to - the fallback location if a spawn point is not part of the blueprint
     * @param island - the island
     * @param env - NETHER or THE_END
     */
    private void pasteNewIsland(Player player, Location to, Island island, Environment env) {
        // Paste then teleport player
        plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon -> {
            // Get the default bundle's nether or end blueprint
            BlueprintBundle bb = plugin.getBlueprintsManager().getDefaultBlueprintBundle(addon);
            if (bb != null) {
                Blueprint bp = plugin.getBlueprintsManager().getBlueprints(addon).get(bb.getBlueprint(env));
                if (bp != null) {
                    new BlueprintPaster(plugin, bp,
                            to.getWorld(),
                            island, () -> new SafeSpotTeleport.Builder(plugin)
                            .entity(player)
                            .location(island.getSpawnPoint(env) == null ? to : island.getSpawnPoint(env))
                            // No need to use portal because there will be no portal on the other end
                            .build());
                } else {
                    plugin.logError("Could not paste default island in nether or end. Is there a nether-island or end-island blueprint?");
                }
            }
        });
    }
}
