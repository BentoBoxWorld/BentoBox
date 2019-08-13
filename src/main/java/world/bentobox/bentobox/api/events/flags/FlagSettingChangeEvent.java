package world.bentobox.bentobox.api.events.flags;

import java.util.UUID;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class FlagSettingChangeEvent extends FlagChangeEvent {

    private final Island island;
    private final boolean setTo;

    /**
     * Event that fires when an island setting flag is changed
     * @param island - island
     * @param player - player changing the flag
     * @param editedFlag - flag that has changed
     * @param setTo - value it was set to
     */
    public FlagSettingChangeEvent(Island island, UUID player, Flag editedFlag, boolean setTo) {
        super(player, editedFlag);
        this.island = island;
        this.setTo = setTo;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * @return the setTo
     */
    public boolean isSetTo() {
        return setTo;
    }

}
