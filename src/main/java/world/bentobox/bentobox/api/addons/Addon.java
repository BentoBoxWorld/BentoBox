package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * Add-on class for BentoBox. Extend this to create an add-on. The operation
 * and methods are very similar to Bukkit's JavaPlugin.
 *
 * @author tastybento, ComminQ_Q
 */
public abstract class Addon implements AddonInterface {

    private static final String ADDON_CONFIG_FILENAME = "config.yml";
    private boolean enabled;
    private AddonDescription description;
    private FileConfiguration config;
    private File dataFolder;
    private File file;

    public Addon() {
        enabled = false;
    }

    public BentoBox getPlugin() {
        return BentoBox.getInstance();
    }

    /**
     * @return the addon's default config file
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            config = loadYamlFile();
        }
        return config;
    }

    /**
     * @return Addon's data folder
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * @return Addon's description
     */
    public AddonDescription getDescription() {
        return description;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return Logger
     */
    public Logger getLogger() {
        return getPlugin().getLogger();
    }

    /**
     * Convenience method to obtain the server
     *
     * @return the server object
     */
    public Server getServer() {
        return getPlugin().getServer();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Load YAML config file
     *
     * @return Yaml File configuration
     */
    private FileConfiguration loadYamlFile() {
        File yamlFile = new File(dataFolder, ADDON_CONFIG_FILENAME);

        YamlConfiguration yamlConfig = null;
        if (yamlFile.exists()) {
            try {
                yamlConfig = new YamlConfiguration();
                yamlConfig.load(yamlFile);
            } catch (Exception e) {
                Bukkit.getLogger().severe(() -> "Could not load config.yml: " + e.getMessage());
            }
        }
        return yamlConfig;
    }

    /**
     * Register a listener for this addon
     *
     * @param listener - listener
     */
    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, BentoBox.getInstance());
    }

    /**
     * Saves the FileConfiguration retrievable by getConfig().
     */
    public void saveConfig() {
        try {
            getConfig().save(new File(dataFolder, ADDON_CONFIG_FILENAME));
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save config!");
        }
    }

    /**
     * Saves the addon's config.yml file to the addon's data folder and loads it. If
     * the file exists already, it will not be replaced.
     */
    public void saveDefaultConfig() {
        saveResource(ADDON_CONFIG_FILENAME, false);
        config = loadYamlFile();
    }

    /**
     * Saves a resource contained in this add-on's jar file to the addon's data
     * folder.
     *
     * @param resourcePath
     *            in jar file
     * @param replace
     *            - if true, will overwrite previous file
     */
    public void saveResource(String resourcePath, boolean replace) {
        saveResource(resourcePath, dataFolder, replace, false);
    }

    /**
     * Saves a resource contained in this add-on's jar file to the destination
     * folder.
     *
     * @param jarResource
     *            in jar file
     * @param destinationFolder
     *            on file system
     * @param replace
     *            - if true, will overwrite previous file
     * @param noPath
     *            - if true, the resource's path will be ignored when saving
     */
    public void saveResource(String jarResource, File destinationFolder, boolean replace, boolean noPath) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    if (in == null) {
                        throw new IllegalArgumentException(
                                "The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
                    }
                    // There are two options, use the path of the resource or not
                    File outFile = new File(destinationFolder, jarResource);
                    if (noPath) {
                        outFile = new File(destinationFolder, outFile.getName());
                    }
                    // Make any dirs that need to be made
                    outFile.getParentFile().mkdirs();
                    if (!outFile.exists() || replace) {
                        java.nio.file.Files.copy(in, outFile.toPath());
                    }
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe(
                    "Could not save from jar file. From " + jarResource + " to " + destinationFolder.getAbsolutePath());
        }
    }

    /**
     * Get the resource from Jar file
     * @param jarResource - jar resource filename
     * @return resource or null if there is a problem
     */
    public InputStream getResource(String jarResource) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    return in;
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not open from jar file. " + jarResource);
        }
        return null;
    }
    /**
     * Set the file that contains this addon
     *
     * @param f
     *            the file to set
     */
    public void setAddonFile(File f) {
        file = f;
    }

    /**
     * Set this addon's data folder
     *
     * @param file - data folder
     */
    public void setDataFolder(File file) {
        dataFolder = file;
    }

    /**
     * Set this addons description
     *
     * @param desc - description
     */
    public void setDescription(AddonDescription desc) {
        description = desc;
    }

    /**
     * Set whether this addon is enabled or not
     *
     * @param enabled - true if enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get Players Manager
     * @return Players manager
     */
    public PlayersManager getPlayers() {
        return getPlugin().getPlayers();
    }

    /**
     * Get Islands Manager
     * @return Islands manager
     */
    public IslandsManager getIslands() {
        return getPlugin().getIslands();
    }

    /**
     * Get the Addon By Name
     * @return Optional Addon
     */
    public Optional<Addon> getAddonByName(String name) {
        return getPlugin().getAddonsManager().getAddonByName(name);
    }

    public void log(String string) {
        getPlugin().log(string);
    }

    public void logWarning(String string) {
        getPlugin().logWarning(string);
    }

    public void logError(String string) {
        getPlugin().logError(string);
    }
}
