package world.bentobox.bentobox.api.events.island;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an island is going to be reset.
 * May be cancelled.
 */
public class IslandResetEvent extends IslandBaseEvent {

    private final @NonNull Set<Island> oldIslands;
    private @NonNull BlueprintBundle blueprintBundle;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public IslandResetEvent(Island island, UUID player, boolean admin, Location location, @NonNull BlueprintBundle blueprintBundle, Set<Island> oldIslands) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
        this.blueprintBundle = blueprintBundle;
        // Create a copy of the old island
        this.oldIslands = oldIslands != null ? new HashSet<>(oldIslands) : null;
    }

    /**
     * @since 2.0.0
     */
    public Set<Island> getOldIslands() {
        return oldIslands;
    }

    /**
     * @since 1.6.0
     */
    @NonNull
    public BlueprintBundle getBlueprintBundle() {
        return blueprintBundle;
    }

    /**
     * @since 1.6.0
     */
    public void setBlueprintBundle(@NonNull BlueprintBundle blueprintBundle) {
        this.blueprintBundle = blueprintBundle;
    }
}