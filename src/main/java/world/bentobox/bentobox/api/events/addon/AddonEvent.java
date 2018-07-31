package world.bentobox.bentobox.api.events.addon;

import world.bentobox.bentobox.api.addons.Addon;

public class AddonEvent {

    public enum Reason {
        ENABLE,
        DISABLE,
        LOAD,
        UNKNOWN
    }

    public static AddonEventBuilder builder() {
        return new AddonEventBuilder();
    }

    public static class AddonEnableEvent extends AddonBaseEvent {
        private AddonEnableEvent(Addon addon) {
            // Final variables have to be declared in the constuctor
            super(addon);
        }
    }
    public static class AddonDisableEvent extends AddonBaseEvent {
        private AddonDisableEvent(Addon addon) {
            // Final variables have to be declared in the constuctor
            super(addon);
        }
    }
    public static class AddonLoadEvent extends AddonBaseEvent {
        private AddonLoadEvent(Addon addon) {
            // Final variables have to be declared in the constuctor
            super(addon);
        }
    }
    public static class AddonGeneralEvent extends AddonBaseEvent {
        private AddonGeneralEvent(Addon addon) {
            // Final variables have to be declared in the constuctor
            super(addon);
        }
    }

    public static class AddonEventBuilder {
        // Here field are NOT final. They are just used for the building.
        private Addon addon;
        private Reason reason = Reason.UNKNOWN;

        public AddonEventBuilder addon(Addon addon) {
            this.addon = addon;
            return this;
        }

        public AddonEventBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public AddonBaseEvent build() {
            switch (reason) {
            case ENABLE:
                return new AddonEnableEvent(addon);
            case DISABLE:
                return new AddonDisableEvent(addon);
            case LOAD:
                return new AddonLoadEvent(addon);
            default:
                return new AddonGeneralEvent(addon);
            }
        }
    }
}
