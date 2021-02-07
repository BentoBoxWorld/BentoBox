package world.bentobox.bentobox.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Abstracts PlayerPortalEvent and EntityPortalEvent
 * @author tastybento
 *
 */
public class PlayerEntityPortalEvent {

    private final EntityPortalEvent epe;
    private final PlayerPortalEvent ppe;

    /**
     * Create a hybrid PlayerEntityPortalEvent
     * @param epe - EntityPortalEvent
     */
    public PlayerEntityPortalEvent(EntityPortalEvent epe) {
        this.ppe = null;
        this.epe = epe;
    }

    /**
     * Create a hybrid PlayerEntityPortalEvent
     * @param ppe - PlayerPortalEvent
     */
    public PlayerEntityPortalEvent(PlayerPortalEvent ppe) {
        this.ppe = ppe;
        this.epe = null;
    }

    /**
     * Returns whether the server will attempt to create a destination portal or not.
     * Only applicable to {@link PlayerPortalEvent}
     * @return whether there should create be a destination portal created
     */
    public boolean getCanCreatePortal() {
        return epe == null ? ppe.getCanCreatePortal() : false;
    }

    /**
     * Returns the entity involved in this event
     * @return Entity who is involved in this event
     */
    @NonNull
    public Entity getEntity() {
        return epe == null ? ppe.getPlayer() : epe.getEntity();
    }

    /**
     * Gets the location this player moved from
     * @return Location the player or entity moved from
     */
    @NonNull
    public Location getFrom() {
        return epe == null ? ppe.getFrom() : epe.getFrom();
    }

    /**
     * Gets the location this player moved to
     * @return Location the player moved to
     */
    @Nullable
    public Location getTo() {
        return epe == null ? ppe.getTo() : epe.getTo();
    }

    /**
     * @return true if constructed with an {@link EntityPortalEvent}
     */
    public boolean isEntityPortalEvent() {
        return epe != null;
    }

    /**
     * @return true if constructed with an {@link PlayerPortalEvent}
     */
    public boolean isPlayerPortalEvent() {
        return ppe != null;
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins
     * If a move or teleport event is cancelled, the player will be moved or teleported back to the Location as defined by getFrom(). This will not fire an event
     * Specified by: setCancelled(...) in Cancellable
     * @param cancel true if you wish to cancel this event
     */
    public void setCancelled(boolean cancel) {
        if (epe == null) {
            ppe.setCancelled(cancel);
        } else {
            epe.setCancelled(cancel);
        }
    }

    /**
     * Sets whether the server should attempt to create a destination portal or not.
     * Only applicable to {@link PlayerPortalEvent}
     * @param canCreatePortal Sets whether there should be a destination portal created
     */
    public void setCanCreatePortal(boolean canCreatePortal) {
        if (ppe != null) {
            ppe.setCanCreatePortal(canCreatePortal);
        }

    }

    /**
     * Set the Block radius to search in for available portals.
     * @param searchRadius the radius in which to search for a portal from the location
     */
    public void setSearchRadius(int searchRadius) {
        if (epe == null) {
            ppe.setSearchRadius(searchRadius);
        } else {
            epe.setSearchRadius(searchRadius);
        }
    }

    /**
     * Sets the location that this player will move to
     * @param to New Location this player or entity will move to
     */
    public void setTo(Location to) {
        if (epe == null) {
            ppe.setTo(to);
        } else {
            epe.setTo(to);
        }
    }

    /**
     * Get island at the from location
     * @return optional island at from location
     */
    public Optional<Island> getIsland() {
        return BentoBox.getInstance().getIslands().getProtectedIslandAt(getFrom());
    }

    /**
     * Get the from world
     * @return from world
     */
    @Nullable
    public World getWorld() {
        return getFrom().getWorld();
    }
}
