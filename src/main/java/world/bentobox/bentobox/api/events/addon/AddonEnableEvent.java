package world.bentobox.bentobox.api.events.addon;

import java.util.Map;

import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.addons.Addon;

/**
 * Called when an addon is enabled
 * @author tastybento
 *
 */
public class AddonEnableEvent extends AddonBaseEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    AddonEnableEvent(Addon addon, Map<String, Object> keyValues) {
        // Final variables have to be declared in the constructor
        super(addon, keyValues);
    }

}