package us.tastybento.bskyblock.api.addons;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.BSModule;
import us.tastybento.bskyblock.managers.CommandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;

import java.io.File;

public abstract class BSAddon implements BSModule {

    private File folder;
    private AddonDescription description;
    private AddonState state;

    public abstract void enable();
    public abstract void disable();
    public abstract void load();
    public abstract void reload();

    public AddonDescription getDescription() {
        return description;
    }

    public AddonState getState()    {   return state;                               }
    public boolean isEnabled()      {   return state == AddonState.ENABLED;         }
    public boolean isDisabled()     {   return state == AddonState.DISABLED;        }
    public boolean isIncompatible() {   return state == AddonState.INCOMPATIBLE;    }

    public CommandsManager getCommandsManager() {
        return BSkyBlock.getPlugin().getCommandsManager();
    }

    public LocalesManager getLocalesManager() {
        return BSkyBlock.getPlugin().getLocalesManager();
    }

    @Override
    public String getIdentifier() {
        return getDescription().getName();
    }

    @Override
    public boolean isAddon() {
        return true;
    }

    @Override
    public File getFolder() {
        if (folder == null) {
            folder = new File(BSkyBlock.getPlugin().getFolder() + "/addons/" + getIdentifier());
        }
        return folder;
    }
}
