package world.bentobox.bentobox.hooks;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Material;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class WorldEditHook extends Hook {

    private WorldEdit instance;

    public WorldEditHook() {
        super("WorldEdit", Material.WOODEN_AXE);
    }

    @Override
    public boolean hook() {
        instance = WorldEdit.getInstance();
        return instance != null;
    }

    @Override
    public String getFailureCause() {
        return null; // The process shouldn't fail
    }
}
