package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Handles respawning back on island
 *
 * @author tastybento
 *
 */
public class IslandRespawnListener extends FlagListener {

    private final Map<UUID, UUID> respawn = new HashMap<>();

    /**
     * Tag players who die in island space and have an island
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        World world = Util.getWorld(e.getEntity().getWorld());
        if (world == null || !getIWM().inWorld(world)) {
            return; // not in the island world
        }
        if (!Flags.ISLAND_RESPAWN.isSetForWorld(world)) {
            return; // world doesn't have the island respawn flag
        }
        if (!getIslands().hasIsland(world, e.getEntity().getUniqueId())
                && !getIslands().inTeam(world, e.getEntity().getUniqueId())) {
            return; // doesn't have an island in this world
        }
        respawn.put(e.getEntity().getUniqueId(), world.getUID());
    }

    /**
     * Place players back on their island if respawn on island is true and active.
     * Valid bed or respawn anchor spawns on an island the player is a member of are
     * honored if the {@link Flags#BED_ANCHOR_RESPAWN} world setting allows it.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final UUID worldUUID = respawn.remove(e.getPlayer().getUniqueId());
        if (worldUUID == null) {
            return; // no respawn world set
        }

        final World world = Bukkit.getWorld(worldUUID);
        if (world == null) {
            return; // world no longer available
        }
        World w = Util.getWorld(world);
        String ownerName = e.getPlayer().getName();
        if (w != null) {
            Island island = getIslands().getIsland(w, User.getInstance(e.getPlayer()));
            if (island != null) {
                ownerName = getPlugin().getPlayers().getName(island.getOwner());
            }
            if (!keepVanillaSpawn(getPlugin(), e, w)) {
                Location respawnLocation = getIslands().getSafeRespawnLocation(world, e.getPlayer().getUniqueId());
                if (respawnLocation != null) {
                    e.setRespawnLocation(respawnLocation);
                } else if (island != null) {
                    // Final fallback: anchor the respawn at the island center so the player
                    // does not appear at world spawn (0,0), then immediately schedule
                    // SafeSpotTeleport to relocate them to the nearest truly safe spot.
                    e.setRespawnLocation(island.getProtectionCenter());
                    Player player = e.getPlayer();
                    Bukkit.getScheduler().runTask(getPlugin(), () ->
                            new SafeSpotTeleport.Builder(getPlugin())
                                    .entity(player)
                                    .island(island)
                                    .cancelIfFail(true)
                                    .build()
                    );
                }
            }
        }
        // Run respawn commands, if any
        Util.runCommands(User.getInstance(e.getPlayer()), ownerName, getIWM().getOnRespawnCommands(world), "respawn");
    }

    /**
     * Checks whether the vanilla-resolved respawn location should be kept instead of
     * sending the player to their island home. It is kept when it comes from a bed or
     * respawn anchor, the {@link Flags#BED_ANCHOR_RESPAWN} world setting is enabled,
     * and the bed/anchor is in the same game mode on an island the player is at least
     * a member of.
     *
     * @param plugin - plugin
     * @param e - the respawn event, carrying the vanilla-resolved respawn location
     * @param w - the game mode's overworld
     * @return true if the vanilla respawn location should be honored
     * @since 3.19.0
     */
    public static boolean keepVanillaSpawn(BentoBox plugin, PlayerRespawnEvent e, World w) {
        if (!e.isBedSpawn() && !e.isAnchorSpawn()) {
            return false;
        }
        if (!Flags.BED_ANCHOR_RESPAWN.isSetForWorld(w)) {
            return false;
        }
        Location loc = e.getRespawnLocation();
        if (!w.equals(Util.getWorld(loc.getWorld()))) {
            return false; // bed/anchor is in a different game mode or a non-game world
        }
        // Only honor spawns inside an island's protected area, consistent with other location checks
        return plugin.getIslands().getProtectedIslandAt(loc)
                .map(i -> i.getMemberSet().contains(e.getPlayer().getUniqueId()))
                .orElse(false);
    }

}
