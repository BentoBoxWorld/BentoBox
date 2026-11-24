package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

import world.bentobox.bentobox.api.user.User;

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
 * A Pladdon is expected to be placed in the {@code /plugins/BentoBox/addons} folder. If the server
 * loads it from the main {@code /plugins} folder instead, it stays inert for this run, moves its jar
 * into the addons folder, and tells the console and any operator who joins that a restart is needed.
 * <p>
 * None of this can run when BentoBox itself is not installed: the addon's main class extends this
 * class, so the server cannot even load it. That case has to be handled in the addon's
 * {@code plugin.yml} (for example with {@code depend: [BentoBox]}), not here.
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
     * Ticks to wait after a player joins before telling them about a misplaced addon, so the message
     * is not lost among the other join messages.
     */
    private static final long JOIN_MESSAGE_DELAY = 40L;

    /**
     * What happened when a misplaced jar was relocated.
     */
    enum MoveResult {
        /** The jar now sits in the addons folder. */
        MOVED,
        /** A jar of the same name was already in the addons folder, so this one was left alone. */
        ALREADY_THERE,
        /** The jar could not be moved; the admin has to do it by hand. */
        FAILED
    }

    /**
     * True when the server loaded this jar from somewhere other than the BentoBox addons folder,
     * typically the main {@code /plugins} folder.
     */
    private boolean misplaced;

    /**
     * Outcome of trying to relocate a misplaced jar, or {@code null} if it was not misplaced.
     */
    private MoveResult moveResult;

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
     * If the jar is not in the {@code /plugins/BentoBox/addons} folder, the Pladdon marks itself as
     * misplaced, moves the jar there and logs what the admin needs to do next.
     * </p>
     */
    @Override
    public void onLoad() {
        if (isInAddonsFolder(getFile())) {
            return;
        }
        misplaced = true;
        moveResult = relocate();
        logMisplaced();
    }

    /**
     * Called when the server enables this plugin.
     * <p>
     * A correctly placed Pladdon is enabled by BentoBox's addon manager, which drives the addon's
     * lifecycle itself, so nothing happens here. A misplaced one repeats its warning once startup has
     * finished (so it is not buried in the boot log) and tells operators about it when they join.
     * </p>
     */
    @Override
    public void onEnable() {
        if (!misplaced) {
            return;
        }
        Bukkit.getScheduler().runTask(this, this::logMisplaced);
        Bukkit.getPluginManager().registerEvents(new MisplacedJoinListener(), this);
    }

    /**
     * Called when the plugin is disabled.
     * <p>
     * This implementation ensures that the associated addon's {@link Addon#onDisable()}
     * method is called. A misplaced Pladdon never enabled its addon, so it has nothing to disable.
     * </p>
     */
    @Override
    public void onDisable() {
        if (misplaced) {
            return;
        }
        Addon addon = getAddon();
        if (addon != null) {
            addon.onDisable();
        }
    }

    /**
     * Whether the server loaded this Pladdon from outside the BentoBox addons folder. A misplaced
     * Pladdon does not run its addon; it only reports the problem until the server is restarted.
     *
     * @return {@code true} if this Pladdon was loaded from the wrong folder
     * @since 3.23.3
     */
    public boolean isMisplaced() {
        return misplaced;
    }

    /**
     * Moves the Pladdon's JAR file from the main plugins folder to the BentoBox addons folder.
     * <p>
     * This is a utility method to help server administrators place the JAR in the correct directory.
     * </p>
     */
    protected void moveJar() {
        relocate();
    }

    /**
     * Moves the jar into the BentoBox addons folder, creating the folder if needed.
     *
     * @return what happened
     */
    MoveResult relocate() {
        File jar = getFile();
        File pluginsFolder = jar.getParentFile();
        if (pluginsFolder == null) {
            return MoveResult.FAILED;
        }
        File addons = new File(pluginsFolder, ADDONS_FOLDER);
        if (!addons.exists() && !addons.mkdirs()) {
            getLogger().severe("BentoBox addons folder could not be made! " + addons.getAbsolutePath());
            return MoveResult.FAILED;
        }
        File to = new File(addons, jar.getName());
        if (to.exists()) {
            return MoveResult.ALREADY_THERE;
        }
        try {
            Files.move(jar, to);
            return MoveResult.MOVED;
        } catch (IOException ex) {
            // Typically Windows, which locks a jar the server has open
            getLogger().log(Level.WARNING, "Could not move " + jar.getName() + ": " + ex.getMessage());
            return MoveResult.FAILED;
        }
    }

    /**
     * Logs a boxed warning explaining where this jar belongs and what to do next.
     */
    private void logMisplaced() {
        String name = getFile().getName();
        String bar = "*".repeat(70);
        getLogger().severe(bar);
        getLogger().severe(name + " is a BentoBox addon, not a standalone plugin.");
        getLogger().severe("Addons go in plugins" + File.separator + ADDONS_FOLDER + ", not in the plugins folder.");
        switch (moveResult == null ? MoveResult.FAILED : moveResult) {
        case MOVED -> getLogger().severe("It has been moved there for you. Restart the server to load it.");
        case ALREADY_THERE -> getLogger().severe(
                "A copy is already in the addons folder. Delete this one from the plugins folder and restart.");
        case FAILED -> getLogger().severe("It could not be moved automatically. Move it there, then restart the server.");
        }
        getLogger().severe(bar);
    }

    /**
     * @param jar the jar file
     * @return true if the jar sits in the {@code BentoBox/addons} folder
     */
    static boolean isInAddonsFolder(File jar) {
        File parent = jar.getParentFile();
        return parent != null && parent.getName().equals("addons") && parent.getParentFile() != null
                && parent.getParentFile().getName().equals("BentoBox");
    }

    /**
     * @return the locale key for the message operators see about a misplaced jar
     */
    String getMisplacedMessageKey() {
        return switch (moveResult == null ? MoveResult.FAILED : moveResult) {
        case MOVED -> "general.misplaced-addon.moved";
        case ALREADY_THERE -> "general.misplaced-addon.duplicate";
        case FAILED -> "general.misplaced-addon.move-failed";
        };
    }

    /**
     * Tells operators who join that this addon is in the wrong folder. Many server owners never
     * read the console, so this is the message most likely to be seen.
     */
    private class MisplacedJoinListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent e) {
            Player player = e.getPlayer();
            if (!player.isOp() && !player.hasPermission("bentobox.admin")) {
                return;
            }
            Bukkit.getScheduler().runTaskLater(Pladdon.this, () -> {
                if (player.isOnline()) {
                    User.getInstance(player).sendMessage(getMisplacedMessageKey(), "[name]", getFile().getName());
                }
            }, JOIN_MESSAGE_DELAY);
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
