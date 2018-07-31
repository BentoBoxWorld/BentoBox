package world.bentobox.bentobox.util.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class SafeTeleportBuilder {

    private BentoBox plugin;
    private Entity entity;
    private int homeNumber = 0;
    private boolean portal = false;
    private String failureMessage = "";
    private Location location;


    public SafeTeleportBuilder(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Set who or what is going to teleport
     * @param entity - entity to teleport
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder entity(Entity entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Set the island to teleport to
     * @param island - island destination
     * @return SafeTeleportBuilder
     */
    public SafeTeleportBuilder island(Island island) {
        this.location = island.getCenter();
        return this;
    }

    /**
     * Set the home number to this number
     * @param homeNumber - home number
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
     * @param failureMessage - failure message to report to user
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
