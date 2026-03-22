package world.bentobox.bentobox.api.events.flags;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import world.bentobox.bentobox.api.events.BentoBoxEvent;

/**
 * This event is fired just before damage is prevented to visitors on an island, if that protection is provided.
 * @author tastybento
 *
 */
public class InvincibleVistorFlagDamageRemovalEvent extends BentoBoxEvent implements Cancellable {
    private final Player player;
    private final DamageCause cause;
    private boolean cancel;

    /**
     * This event is fired just before damage is prevented to visitors on an island, if that protection is provided.
     * @param player player being protected
     * @param cause damage cause
     */
    public InvincibleVistorFlagDamageRemovalEvent(Player player, DamageCause cause) {
        this.player = player;
        this.cause = cause;
    }
    @Override
    public boolean isCancelled() {
        return cancel;
    }
    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }
    /**
     * @return the cause
     */
    public DamageCause getCause() {
        return cause;
    }
}

