package world.bentobox.bentobox.blueprints.converter;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import world.bentobox.bentobox.BentoBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @since 1.6.0
 * @author CustomEntity
 */
public class BlueprintSchematicConverter {

    private File blueprintFile;

    public BlueprintSchematicConverter(File blueprintFile) {
        if(!BentoBox.getInstance().getHooks().getHook("WorldEdit").isPresent()) {
            BentoBox.getInstance().logError("WorldEdit must be installed to use that class !");
            return;
        }
        this.blueprintFile = blueprintFile;
    }

    public Clipboard convertBlueprintToSchematic() {
        Clipboard clipboard = null;
        try {
            clipboard = ClipboardFormats.findByFile(blueprintFile).getReader(new FileInputStream(blueprintFile)).read();
        } catch (IOException e) {
            BentoBox.getInstance().logWarning("Error while trying to convert blueprint format to schematic format.");
            e.printStackTrace();
        }
        return clipboard;
    }

    public File getBlueprintFile() {
        return blueprintFile;
    }
}
