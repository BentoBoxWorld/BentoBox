package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

/**
 * @author tastybento
 *
 */
public abstract class Pladdon extends JavaPlugin {

    private static final String ADDONS_FOLDER = "BentoBox/addons";

    public abstract Addon getAddon();

    @Override
    public void onLoad() {
        String parentFolder = getFile().getParent();
        if (parentFolder == null || !parentFolder.endsWith(ADDONS_FOLDER)) {
            // Jar is in the wrong place. Let's move it
            moveJar();
        }
    }

    public void moveJar() {
        getLogger().severe(getFile().getName() + " must be in the BentoBox/addons folder! Trying to move it there...");
        File addons = new File(getFile().getParent(), ADDONS_FOLDER);
        if (addons.exists() || addons.mkdirs()) {
            File to = new File(addons, getFile().getName());
            if (!to.exists()) {
                try {
                    Files.move(getFile(), to);
                    getLogger().severe(getFile().getName() + " moved successfully.");

                } catch (IOException ex) {
                    getLogger().severe("Failed to move it. " + ex.getMessage());
                    getLogger().severe("Move " + getFile().getName() + " manually into the BentoBox/addons folder. Then restart server.");
                }
            } else {
                getLogger().warning(getFile().getName() + " already is in the addons folder. Delete the one in the plugins folder.");
            }
        } else {
            getLogger().severe("BentoBox addons folder could not be made! " + addons.getAbsolutePath());
        }

    }
}
