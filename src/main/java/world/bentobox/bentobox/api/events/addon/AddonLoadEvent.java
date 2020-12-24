package world.bentobox.bentobox.api.events.addon;

import java.util.Map;

import org.bukkit.event.HandlerList;

import world.bentobox.bentobox.api.addons.Addon;

public class AddonLoadEvent extends AddonBaseEvent {
    AddonLoadEvent(Addon addon, Map<String, Object> keyValues) {
        // Final variables have to be declared in the constructor
        super(addon, keyValues);
    }
    private final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}