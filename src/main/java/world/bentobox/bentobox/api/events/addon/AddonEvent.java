package world.bentobox.bentobox.api.events.addon;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.addons.Addon;

public class AddonEvent {


    public enum Reason {
        ENABLE,
        DISABLE,
        LOAD,
        UNKNOWN
    }


    /**
     * @return Addon event builder
     */
    public AddonEventBuilder builder() {
        return new AddonEventBuilder();
    }

    public class AddonEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private Addon addon;
        private Reason reason = Reason.UNKNOWN;
        private Map<String, Object> keyValues = new HashMap<>();

        /**
         * Add a map of key-value pairs to the event. Use this to transfer data from the addon to the external world.
         * @param keyValues - map
         * @return AddonEvent
         */
        public AddonEventBuilder keyValues(Map<String, Object> keyValues) {
            this.keyValues = keyValues;
            return this;
        }

        public AddonEventBuilder addon(Addon addon) {
            this.addon = addon;
            return this;
        }

        public AddonEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        private AddonBaseEvent getEvent() {
            return switch (reason) {
            case ENABLE -> new AddonEnableEvent(addon, keyValues);
            case DISABLE -> new AddonDisableEvent(addon, keyValues);
            case LOAD -> new AddonLoadEvent(addon, keyValues);
            default -> new AddonGeneralEvent(addon, keyValues);
            };
        }

        /**
         * Build and fire event
         * @return event - deprecated event. To obtain the new event use {@link AddonBaseEvent#getNewEvent()}
         */
        public AddonBaseEvent build() {
            // Call new event
            AddonBaseEvent newEvent = getEvent();
            Bukkit.getPluginManager().callEvent(newEvent);
            return newEvent;
        }
    }
}
