package world.bentobox.bentobox.hooks;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.worldedit.BlueprintClipboardFormat;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class WorldEditHook extends Hook {

    private WorldEdit instance;
    private BlueprintClipboardFormat clipboardFormat;

    public WorldEditHook() {
        super("WorldEdit", Material.WOODEN_AXE);
    }

    @Override
    public boolean hook() {
        try {
            instance = WorldEdit.getInstance();
            clipboardFormat = new BlueprintClipboardFormat();
            ClipboardFormats.registerClipboardFormat(clipboardFormat);
        } catch (Exception | NoClassDefFoundError | NoSuchMethodError e) {
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
