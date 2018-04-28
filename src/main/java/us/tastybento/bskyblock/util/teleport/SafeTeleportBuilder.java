package us.tastybento.bskyblock.util.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;

public class SafeTeleportBuilder {

    private BSkyBlock plugin;
    private Entity entity;
    private int homeNumber = 0;
    private boolean portal = false;
    private String failureMessage = "";
    private Location location;


    public SafeTeleportBuilder(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Set who or what is going to teleport
     * @param entity
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder entity(Entity entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Set the island to teleport to
     * @param island
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder island(Island island) {
        this.location = island.getCenter();
        return this;
    }

    /**
     * Set the home number to this number
     * @param homeNumber
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder homeNumber(int homeNumber) {
        this.homeNumber = homeNumber;
        return this;
    }

    /**
     * This is a portal teleportation
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder portal() {
        this.portal = true;
        return this;
    }

    /**
     * Set the failure message if this teleport cannot happen
     * @param failureMessage
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder failureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    /**
     * Set the desired location
     * @param location - the location
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder location(Location location) {
        this.location = location;
        return this;
    }

    /**
     * Try to teleport the player
     * @return SafeSpotTeleport
     */
    public SafeSpotTeleport build() {      
        // Error checking
        if (entity == null) {
            plugin.logError("Attempt to safe teleport a null entity!");
            return null;
        }
        if (location == null) {
            plugin.logError("Attempt to safe teleport to a null location!");
            return null;
        }
        if (failureMessage.isEmpty() && entity instanceof Player) {
            failureMessage = User.getInstance(entity).getTranslation("general.errors.warp-not-safe");
        }
        return new SafeSpotTeleport(plugin, entity, location, failureMessage, portal, homeNumber);
    }

}
