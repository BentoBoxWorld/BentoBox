package world.bentobox.bentobox.listeners;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

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
    private Set<UUID> inPortal;

    public PortalTeleportationListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        inPortal = new HashSet<>();
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        // Remove player from inPortal after a teleport
        inPortal.remove(e.getPlayer().getUniqueId());
    }

    /**
     * Fires the event if nether or end is disabled at the system level
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPortal(PlayerMoveEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (inPortal.contains(uuid) || !plugin.getIWM().inWorld(Util.getWorld(e.getFrom().getWorld()))) {
            return;
        }
        if (!Bukkit.getServer().getAllowNether() && e.getPlayer().getLocation().getBlock().getType().equals(Material.NETHER_PORTAL)) {
            inPortal.add(uuid);
            // Schedule a time
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Check again if still in portal
                if (e.getPlayer().getLocation().getBlock().getType().equals(Material.NETHER_PORTAL)) {
                    PlayerPortalEvent en = new PlayerPortalEvent(e.getPlayer(), e.getPlayer().getLocation(), null, TeleportCause.NETHER_PORTAL, 0, false, 0);
                    if (!this.onNetherPortal(en)) {
                        // Failed
                        inPortal.remove(uuid);
                    }
                } else {
                    inPortal.remove(uuid);
                }
            }, 40);
            return;
        }
        if (!Bukkit.getServer().getAllowEnd() && e.getPlayer().getLocation().getBlock().getType().equals(Material.END_PORTAL)) {
            PlayerPortalEvent en = new PlayerPortalEvent(e.getPlayer(), e.getPlayer().getLocation(), null, TeleportCause.END_PORTAL, 0, false, 0);
            this.onEndIslandPortal(en);
            return;
        }
    }

    /**
     * Handles non-player portal use.
     * Currently disables portal use by entities to prevent dupe glitching.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) {
        if (plugin.getIWM().inWorld(e.getFrom())) {
            // Disable entity portal transfer due to dupe glitching
            e.setCancelled(true);
        }
    }

    /**
     * Handles end portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public boolean onEndIslandPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.END_PORTAL) {
            return false;
        }
        World fromWorld = e.getFrom().getWorld();
        World overWorld = Util.getWorld(fromWorld);

        if (fromWorld == null || !plugin.getIWM().inWorld(overWorld)) {
            // Do nothing special
            return false;
        }

        // 1.14.4 requires explicit cancellation to prevent teleporting to the normal nether
        if (!plugin.getIWM().isEndGenerate(overWorld)) {
            e.setCancelled(true);
            return false;
        }

        // STANDARD END
        if (!plugin.getIWM().isEndIslands(overWorld)) {
            if (fromWorld.getEnvironment() != Environment.THE_END) {
                if (Bukkit.getAllowEnd()) {
                    // To Standard end
                    e.setTo(plugin.getIWM().getEndWorld(overWorld).getSpawnLocation());
                } else {
                    new SafeSpotTeleport.Builder(plugin)
                    .entity(e.getPlayer())
                    .location(plugin.getIWM().getEndWorld(overWorld).getSpawnLocation())
                    .thenRun(() -> inPortal.remove(e.getPlayer().getUniqueId()))
                    .build();
                }
            }
            // From standard end - check if player has an island to go to
            else if (plugin.getIslands().hasIsland(overWorld, e.getPlayer().getUniqueId())
                    || plugin.getIslands().inTeam(overWorld, e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                plugin.getIslands().homeTeleportAsync(overWorld, e.getPlayer());
            }
            // No island, so just do nothing
            return false;
        }

        // FROM END
        // If entering an ender portal in the End.
        if (fromWorld.getEnvironment() == Environment.THE_END) {
            // If this is from the island nether, then go to the same vector, otherwise try island home location
            Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(e.getFrom().toVector().toLocation(overWorld));
            e.setCancelled(true);
            // Else other worlds teleport to the overworld
            new SafeSpotTeleport.Builder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .portal()
            .build();
            return true;
        }
        // TO END

        World endWorld = plugin.getIWM().getEndWorld(overWorld);
        // If this is to island End, then go to the same vector, otherwise try spawn
        Optional<Island> optionalIsland = plugin.getIslands().getIslandAt(e.getFrom());
        Location to = optionalIsland.map(i -> i.getSpawnPoint(Environment.THE_END)).orElse(e.getFrom().toVector().toLocation(endWorld));
        e.setCancelled(true);
        // Check if there is a missing end island
        if (plugin.getIWM().isPasteMissingIslands(overWorld)
                && !plugin.getIWM().isUseOwnGenerator(overWorld)
                && plugin.getIWM().isEndGenerate(overWorld)
                && plugin.getIWM().isEndIslands(overWorld)
                && plugin.getIWM().getEndWorld(overWorld) != null
                && optionalIsland.filter(i -> !i.hasEndIsland())
                .map(i -> {
                    // No end island present so paste the default one
                    pasteNewIsland(e.getPlayer(), to, i, Environment.THE_END);
                    return true;
                }).orElse(false)) {
            // We are done here
            return true;
        }
        // Set player's velocity and fall distance to 0
        e.getPlayer().setVelocity(new Vector(0,0,0));
        e.getPlayer().setFallDistance(0);

        // Else other worlds teleport to the end
        // Set player's velocity to zero one tick after cancellation
        // Teleport
        new SafeSpotTeleport.Builder(plugin)
        .entity(e.getPlayer())
        .location(to)
        .thenRun(() -> {
            e.getPlayer().setVelocity(new Vector(0,0,0));
            e.getPlayer().setFallDistance(0);
        })
        .build();
        return true;
    }

    /**
     * Handles nether portals.
     * @param e - event
     * @return false if teleport does not happen, true if it does
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true) // Use HIGH to allow Multiverse first shot
    public boolean onNetherPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.NETHER_PORTAL) {
            return false;
        }
        World fromWorld = e.getFrom().getWorld();
        World overWorld = Util.getWorld(fromWorld);
        if (fromWorld == null || !plugin.getIWM().inWorld(overWorld)) {
            // Do nothing special
            return false;
        }
        // 1.14.4 requires explicit cancellation to prevent teleporting to the normal nether
        if (!plugin.getIWM().isNetherGenerate(overWorld)) {
            e.setCancelled(true);
            return false;
        }
        // STANDARD NETHER
        if (!plugin.getIWM().isNetherIslands(overWorld)) {
            if (fromWorld.getEnvironment() != Environment.NETHER) {
                if (Bukkit.getServer().getAllowNether()) {
                    // To Standard Nether
                    e.setTo(plugin.getIWM().getNetherWorld(overWorld).getSpawnLocation());
                } else {
                    // TODO - teleport player
                    new SafeSpotTeleport.Builder(plugin)
                    .entity(e.getPlayer())
                    .location(plugin.getIWM().getNetherWorld(fromWorld).getSpawnLocation())
                    .portal()
                    .build();
                }
            }
            // From standard nether
            else {
                e.setCancelled(true);
                plugin.getIslands().homeTeleportAsync(overWorld, e.getPlayer()).thenAccept(b -> inPortal.remove(e.getPlayer().getUniqueId()));
            }
            return true;
        }
        // FROM NETHER
        // If entering a nether portal in the nether, teleport to portal in overworld if there is one
        if (fromWorld.getEnvironment() == Environment.NETHER) {
            // If this is from the island nether, then go to the same vector, otherwise try island home location
            Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(e.getFrom().toVector().toLocation(overWorld));
            e.setCancelled(true);
            // Else other worlds teleport to the nether
            new SafeSpotTeleport.Builder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .portal()
            .thenRun(() -> inPortal.remove(e.getPlayer().getUniqueId()))
            .build();
            return true;
        }
        // TO NETHER
        World nether = plugin.getIWM().getNetherWorld(overWorld);
        // If this is to island nether, then go to the same vector, otherwise try spawn
        Optional<Island> optionalIsland = plugin.getIslands().getIslandAt(e.getFrom());
        Location to = optionalIsland.map(i -> i.getSpawnPoint(Environment.NETHER)).orElse(e.getFrom().toVector().toLocation(nether));
        e.setCancelled(true);
        // Check if there is an island there or not
        if (plugin.getIWM().isPasteMissingIslands(overWorld) &&
                !plugin.getIWM().isUseOwnGenerator(overWorld)
                && plugin.getIWM().isNetherGenerate(overWorld)
                && plugin.getIWM().isNetherIslands(overWorld)
                && plugin.getIWM().getNetherWorld(overWorld) != null
                && optionalIsland.filter(i -> !i.hasNetherIsland()).map(i -> {
                    // No nether island present so paste the default one
                    pasteNewIsland(e.getPlayer(), to, i, Environment.NETHER);
                    return true;
                }).orElse(false)) {
            // All done here
            return true;
        }
        // Else other worlds teleport to the nether
        new SafeSpotTeleport.Builder(plugin)
        .entity(e.getPlayer())
        .location(to)
        .portal()
        .thenRun(() -> inPortal.remove(e.getPlayer().getUniqueId()))
        .build();
        return true;
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
                            island).paste().thenAccept(b -> new SafeSpotTeleport.Builder(plugin)
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
