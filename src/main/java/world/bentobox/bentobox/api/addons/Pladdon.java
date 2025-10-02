package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

/**
 * A wrapper class that allows a BentoBox {@link Addon} to be loaded as a Bukkit {@link JavaPlugin}.
 * <p>
 * The name "Pladdon" is a portmanteau of "Plugin" and "Addon".
 * <p>
 * This class serves two main purposes:
 * <ol>
 *     <li>It allows other Bukkit plugins to declare a dependency on a BentoBox addon in their
 *     {@code plugin.yml}, ensuring the correct load order.</li>
 *     <li>It provides a standard Bukkit plugin entry point, which can make it easier for
 *     other plugins to access the addon's API.</li>
 * </ol>
 * A Pladdon is expected to be placed in the {@code /plugins/BentoBox/addons} folder. If it is
 * placed in the main {@code /plugins} folder, it will attempt to move itself.
 *
 * @author tastybento
 * @since 1.0
 */
public abstract class Pladdon extends JavaPlugin {

    /**
     * The path to the BentoBox addons folder, relative to the plugins directory.
     */
    private static final String ADDONS_FOLDER = "BentoBox" + File.separator + "addons";

    /**
     * This method must be implemented to provide an instance of the associated {@link Addon}.
     * This instance is used by BentoBox's addon manager.
     *
     * @return A new instance of the addon.
     */
    public abstract Addon getAddon();

    /**
     * Called when the plugin is loaded.
     * <p>
     * This implementation checks if the Pladdon's JAR file is in the correct
     * {@code /plugins/BentoBox/addons} directory. If not, it will attempt to move it.
     * The moving logic is currently commented out.
     * </p>
     */
    @Override
    public void onLoad() {
        String parentFolder = getFile().getParent();
        // Check if the JAR is in the correct addons folder.
        if (parentFolder == null || !parentFolder.endsWith(ADDONS_FOLDER)) {
            // The JAR is in the wrong location. It should be in the /plugins/BentoBox/addons/ folder.
            // The logic to move the jar is commented out by default.
            // moveJar();
        }
    }

    /**
     * Called when the plugin is disabled.
     * <p>
     * This implementation ensures that the associated addon's {@link Addon#onDisable()}
     * method is called.
     * </p>
     */
    @Override
    public void onDisable() {
        Addon addon = getAddon();
        if (addon != null) {
            addon.onDisable();
        }
    }

    /**
     * Moves the Pladdon's JAR file from the main plugins folder to the BentoBox addons folder.
     * <p>
     * This is a utility method to help server administrators place the JAR in the correct directory.
     * </p>
     */
    protected void moveJar() {
        getLogger().severe(getFile().getName() + " must be in the " + ADDONS_FOLDER + " folder! Trying to move it there...");
        File addons = new File(getFile().getParent(), ADDONS_FOLDER);
        // Create the addons folder if it doesn't exist.
        if (addons.exists() || addons.mkdirs()) {
            File to = new File(addons, getFile().getName());
            if (!to.exists()) {
                try {
                    // Move the file.
                    Files.move(getFile(), to);
                    getLogger().severe(getFile().getName() + " moved successfully.");

                } catch (IOException ex) {
                    getLogger().severe("Failed to move it. " + ex.getMessage());
                    getLogger().severe("Move " + getFile().getName() + " manually into the " + ADDONS_FOLDER + " folder. Then restart server.");
                }
            } else {
                getLogger().warning(getFile().getName() + " already is in the addons folder. Delete the one in the plugins folder.");
            }
        } else {
            getLogger().severe("BentoBox addons folder could not be made! " + addons.getAbsolutePath());
        }

    }


    /**
     * Manually sets the enabled state of this Pladdon to {@code true}.
     * <p>
     * This is necessary because BentoBox's {@link world.bentobox.bentobox.managers.AddonsManager}
     * controls the lifecycle of addons, which means the standard Bukkit {@link #onEnable()}
     * is not called for the Pladdon wrapper. This method allows the addon manager to mark
     * the Pladdon as enabled after it has successfully loaded the underlying addon.
     * </p>
     */
    public void setEnabled() {
        this.setEnabled(true);
    }
}
