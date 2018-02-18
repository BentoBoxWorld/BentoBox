package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.Island;

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
     * @param island
     * @param player - the player
     * @param editedFlag
     * @param setTo
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
