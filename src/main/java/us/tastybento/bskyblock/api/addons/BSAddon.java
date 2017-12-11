package us.tastybento.bskyblock.api.addons;

public abstract class BSAddon {

    private AddonDescription description;
    private AddonState state;

    public abstract void enable();
    public abstract void disable();
    public abstract void load();

    public AddonDescription getDescription() {
        return description;
    }

    public AddonState getState()    {   return state;                               }
    public boolean isEnabled()      {   return state == AddonState.ENABLED;         }
    public boolean isDisabled()     {   return state == AddonState.DISABLED;        }
    public boolean isIncompatible() {   return state == AddonState.INCOMPATIBLE;    }
}
