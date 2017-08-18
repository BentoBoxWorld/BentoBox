package us.tastybento.bskyblock.api.events.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;

/**
 * This event is fired when a player changes a setting on his island
 * <p>
 * Canceling this event will result in canceling the change.
 *
 * @author Poslovitch
 * @since 1.0
 */
public class SettingChangeEvent extends IslandEvent {
    private final UUID player;
    private final SettingsFlag editedSetting;
    private final boolean setTo;

    /**
     * @param island
     * @param player
     * @param editedSetting
     * @param setTo
     */
    public SettingChangeEvent(Island island, UUID player, SettingsFlag editedSetting, boolean setTo) {
        super(island);
        this.player = player;
        this.editedSetting = editedSetting;
        this.setTo = setTo;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * @return the edited setting
     */
    public SettingsFlag getSetting() {
        return this.editedSetting;
    }

    /**
     * @return enabled/disabled
     */
    public boolean getSetTo() {
        return this.setTo;
    }
}
