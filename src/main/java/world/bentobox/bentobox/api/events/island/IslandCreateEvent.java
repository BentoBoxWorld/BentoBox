package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Fired when an island is going to be created.
 * May be cancelled.
 *
 */
public class IslandCreateEvent extends IslandBaseEvent {
    private @NonNull BlueprintBundle blueprintBundle;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    IslandCreateEvent(Island island, UUID player, boolean admin, Location location, @NonNull BlueprintBundle blueprintBundle) {
        // Final variables have to be declared in the constructor
        super(island, player, admin, location);
        this.blueprintBundle = blueprintBundle;
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
