package world.bentobox.bentobox.hooks;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.Nullable;

import com.sk89q.worldedit.WorldEdit;

import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.worldedit.BlueprintClipboardFormat;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class WorldEditHook extends Hook {

    public WorldEditHook() {
        super("WorldEdit", Material.WOODEN_AXE);
    }

    @Override
    public boolean hook() {

        WorldEdit instance;
        try {
            instance = WorldEdit.getInstance();
            new BlueprintClipboardFormat();
        } catch (Exception e) {
            return false;
        }

        return instance != null;

    }

    @Override
    @Nullable
    public String getFailureCause() {
        return null; // The process shouldn't fail
    }
}
