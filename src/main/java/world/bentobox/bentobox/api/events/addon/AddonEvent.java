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

    public class AddonEnableEvent extends AddonBaseEvent {
        private AddonEnableEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }
    public class AddonDisableEvent extends AddonBaseEvent {
        private AddonDisableEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }
    public class AddonLoadEvent extends AddonBaseEvent {
        private AddonLoadEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }
    public class AddonGeneralEvent extends AddonBaseEvent {
        private AddonGeneralEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
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
            switch (reason) {
            case ENABLE:
                return new AddonEnableEvent(addon, keyValues);
            case DISABLE:
                return new AddonDisableEvent(addon, keyValues);
            case LOAD:
                return new AddonLoadEvent(addon, keyValues);
            default:
                return new AddonGeneralEvent(addon, keyValues);
            }
        }

        /**
         * Build and fire event
         * @return event
         */
        public AddonBaseEvent build() {
            AddonBaseEvent e = getEvent();
            Bukkit.getPluginManager().callEvent(e);
            return e;
        }
    }
}
