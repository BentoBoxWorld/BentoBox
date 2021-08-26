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

    /**
     * @deprecated This event is moving to its own class.
     * Use {@link world.bentobox.bentobox.api.events.addon.AddonEnableEvent}
     */
    @Deprecated
    public class AddonEnableEvent extends AddonBaseEvent {
        private AddonEnableEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }

    /**
     * @deprecated This event is moving to its own class.
     * Use {@link world.bentobox.bentobox.api.events.addon.AddonDisableEvent}
     */
    @Deprecated
    public class AddonDisableEvent extends AddonBaseEvent {
        private AddonDisableEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }

    /**
     * @deprecated This event is moving to its own class.
     * Use {@link world.bentobox.bentobox.api.events.addon.AddonLoadEvent}
     */
    @Deprecated
    public class AddonLoadEvent extends AddonBaseEvent {
        private AddonLoadEvent(Addon addon, Map<String, Object> keyValues) {
            // Final variables have to be declared in the constructor
            super(addon, keyValues);
        }
    }

    /**
     * @deprecated This event is moving to its own class.
     * Use {@link world.bentobox.bentobox.api.events.addon.AddonGeneralEvent}
     */
    @Deprecated
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

        private AddonBaseEvent getDeprecatedEvent() {
            return switch (reason) {
                case ENABLE -> new AddonEnableEvent(addon, keyValues);
                case DISABLE -> new AddonDisableEvent(addon, keyValues);
                case LOAD -> new AddonLoadEvent(addon, keyValues);
                default -> new AddonGeneralEvent(addon, keyValues);
            };
        }

        private AddonBaseEvent getEvent() {
            return switch (reason) {
                case ENABLE -> new world.bentobox.bentobox.api.events.addon.AddonEnableEvent(addon, keyValues);
                case DISABLE -> new world.bentobox.bentobox.api.events.addon.AddonDisableEvent(addon, keyValues);
                case LOAD -> new world.bentobox.bentobox.api.events.addon.AddonLoadEvent(addon, keyValues);
                default -> new world.bentobox.bentobox.api.events.addon.AddonGeneralEvent(addon, keyValues);
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
            // Get the old event
            AddonBaseEvent e = getDeprecatedEvent();
            e.setNewEvent(newEvent);
            // Call deprecated event
            Bukkit.getPluginManager().callEvent(e);
            return e;
        }
    }
}
