package world.bentobox.bentobox.api.events.flags;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.HandlerList;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class FlagWorldSettingChangeEvent extends FlagChangeEvent {

    private final World world;
    private final boolean setTo;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Event that fires when a world setting is changed
     * @param world - world
     * @param player - player changing the flag
     * @param editedFlag - flag that has changed
     * @param setTo - value it was set to
     */
    public FlagWorldSettingChangeEvent(World world, UUID player, Flag editedFlag, boolean setTo) {
        super(player, editedFlag);
        this.world = world;
        this.setTo = setTo;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the setTo
     */
    public boolean isSetTo() {
        return setTo;
    }

}
