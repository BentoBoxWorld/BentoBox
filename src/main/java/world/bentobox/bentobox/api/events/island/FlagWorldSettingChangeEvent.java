package world.bentobox.bentobox.api.events.island;

import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.api.events.BentoBoxEvent;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class FlagWorldSettingChangeEvent extends BentoBoxEvent {

    private final World world;
    private final UUID player;
    private final Flag editedFlag;
    private final boolean setTo;

    /**
     * Event that fires when a world setting is changed
     * @param world - world
     * @param player - player changing the flag
     * @param editedFlag - flag that has changed
     * @param setTo - value it was set to
     */
    public FlagWorldSettingChangeEvent(World world, UUID player, Flag editedFlag, boolean setTo) {
        this.world = world;
        this.player = player;
        this.editedFlag = editedFlag;
        this.setTo = setTo;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
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

    /**
     * @return the setTo
     */
    public boolean isSetTo() {
        return setTo;
    }

}
