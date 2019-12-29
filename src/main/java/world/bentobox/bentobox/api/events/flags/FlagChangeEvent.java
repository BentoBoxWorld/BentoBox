package world.bentobox.bentobox.api.events.flags;

import java.util.UUID;

import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.bentobox.api.flags.Flag;

/**
 *
 * @author Poslovitch
 * @since 1.6.0
 */
public abstract class FlagChangeEvent extends BentoBoxEvent {

    private final UUID player;
    private final Flag editedFlag;

    /**
     * @param player - player changing the flag
     * @param editedFlag - flag that has changed
     */
    public FlagChangeEvent(UUID player, Flag editedFlag) {
        this.player = player;
        this.editedFlag = editedFlag;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * @return the editedFlag
     */
    public Flag getEditedFlag() {
        return editedFlag;
    }
}
