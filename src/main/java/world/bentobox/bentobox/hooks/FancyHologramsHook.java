package world.bentobox.bentobox.hooks;

import org.bukkit.Material;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides copy and pasting of FancyHolograms in blueprints
 *
 * @author tastybento
 * @since 3.2.0
 */
public class FancyHologramsHook extends Hook {

    public FancyHologramsHook() {
        super("FancyHolograms", Material.END_PORTAL);
    }


    @Override
    public boolean hook() {
        boolean hooked = this.isPluginAvailable();
        if (!hooked) {
            BentoBox.getInstance().logError("Could not hook into FancyHolograms");
        }
        return hooked; // The hook process shouldn't fail
    }

    @Override
    public String getFailureCause() {
        return null; // The hook process shouldn't fail
    }

}
