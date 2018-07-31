package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;

/**
 * This event is fired when a player changes a flag on his island
 * <p>
 * Canceling this event will result in canceling the change.
 *
 * @author Poslovitch
 * @since 1.0
 */
public class FlagChangeEvent extends IslandBaseEvent {
    private final UUID player;
    private final Flag editedFlag;
    private final boolean setTo;

    /**
     * @param island - island
     * @param player - the player
     * @param editedFlag - flag edited
     * @param setTo - new value
     */
    public FlagChangeEvent(Island island, UUID player, Flag editedFlag, boolean setTo) {
        super(island);
        this.player = player;
        this.editedFlag = editedFlag;
        this.setTo = setTo;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * @return the edited flag
     */
    public Flag getFlag() {
        return editedFlag;
    }

    /**
     * @return enabled/disabled
     */
    public boolean getSetTo() {
        return setTo;
    }
}
