package us.tastybento.bskyblock.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;

public class SafeTeleportBuilder {

    private BSkyBlock plugin;
    private Entity entity;
    private int homeNumber = 0;
    private boolean portal = false;
    private String failureMessage = "general.errors.warp-not-safe";
    private Location location;


    public SafeTeleportBuilder(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Set who or what is going to teleport
     * @param entity
     * @return
     */
    public SafeTeleportBuilder entity(Entity entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Set the island to teleport to
     * @param island
     * @return
     */
    public SafeTeleportBuilder island(Island island) {
        this.location = island.getCenter();
        return this;
    }

    /**
     * Set the home number to this number
     * @param homeNumber
     * @return
     */
    public SafeTeleportBuilder homeNumber(int homeNumber) {
        this.homeNumber = homeNumber;
        return this;
    }

    /**
     * This is a portal teleportation
     * @param setHome
     * @return
     */
    public SafeTeleportBuilder portal() {
        this.portal = true;
        return this;
    }

    /**
     * Set the failure message if this teleport cannot happen
     * @param failureMessage
     * @return
     */
    public SafeTeleportBuilder failureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    /**
     * Set the desired location
     * @param location
     * @return
     */
    public SafeTeleportBuilder location(Location location) {
        this.location = location;
        return this;
    }

    /**
     * Try to teleport the player
     * @return
     */
    public SafeSpotTeleport build() {
        return new SafeSpotTeleport(plugin, entity, location, failureMessage, portal, homeNumber);
    }


}
