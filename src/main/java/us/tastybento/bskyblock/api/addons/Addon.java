package us.tastybento.bskyblock.api.addons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Add-on class for BSkyBlock. Extend this to create an add-on.
 * The operation and methods are very similar to Bukkit's JavaPlugin.
 *
 * @author tastybento, ComminQ_Q
 */
public abstract class Addon implements AddonInterface {

    private static final String ADDON_CONFIG_FILENAME = "config.yml";
    private static final boolean DEBUG = false;
    private boolean enabled;
    private AddonDescription description;
    private FileConfiguration config;
    private File dataFolder;
    private File file;

    public Addon() {
        this.enabled = false;
    }

    public BSkyBlock getBSkyBlock(){
        return BSkyBlock.getInstance();
    }

    /**
     * @return the addon's default config file
     */
    public FileConfiguration getConfig() {
        config = loadYamlFile(ADDON_CONFIG_FILENAME);
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
        return getBSkyBlock().getLogger();
    }

    /**
     * Convenience method to obtain the server
     * @return the server object
     */
    public Server getServer() {
        return getBSkyBlock().getServer();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Load a YAML file
     * @param file
     * @return Yaml File configuration
     */
    private FileConfiguration loadYamlFile(String file) {
        File yamlFile = new File(dataFolder, file);

        YamlConfiguration config = null;
        if (yamlFile.exists()) {
            try {
                config = new YamlConfiguration();
                config.load(yamlFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return config;
    }

    /**
     * Register a listener for this addon
     * @param listener
     */
    public void registerListener(Listener listener){
        BSkyBlock.getInstance().getServer().getPluginManager().registerEvents(listener, BSkyBlock.getInstance());
    }

    /**
     * Saves the default config file
     */
    public void saveConfig() {
        try {
            this.config.save(new File(dataFolder, ADDON_CONFIG_FILENAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the addon's config.yml file to the addon's data folder and loads it.
     * If the file exists already, it will not be replaced.
     */
    public void saveDefaultConfig() {
        saveResource(ADDON_CONFIG_FILENAME, false);
        config = loadYamlFile(ADDON_CONFIG_FILENAME);
    }

    /**
     * Saves a resource contained in this add-on's jar file to the addon's data folder.
     * @param resourcePath in jar file
     * @param replace - if true, will overwrite previous file
     */
    public void saveResource(String resourcePath, boolean replace) {
        saveResource(resourcePath, dataFolder, replace, false);
    }

    /**
     * Saves a resource contained in this add-on's jar file to the destination folder.
     * @param jarResource in jar file
     * @param destinationFolder on file system
     * @param replace - if true, will overwrite previous file
     * @param noPath - if true, the resource's path will be ignored when saving
     */
    public void saveResource(String jarResource, File destinationFolder, boolean replace, boolean noPath) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry config = jar.getJarEntry(jarResource);
            if (config != null) {
                try (InputStream in = jar.getInputStream(config)) {
                    if (in == null) {
                        jar.close();
                        throw new IllegalArgumentException("The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
                    }
                    // There are two options, use the path of the resource or not
                    File outFile = new File(destinationFolder, jarResource);
                    if (noPath) {
                        outFile = new File(destinationFolder, outFile.getName());
                    }
                    // Make any dirs that need to be made
                    outFile.getParentFile().mkdirs();
                    if (DEBUG) {
                        Bukkit.getLogger().info("DEBUG: outFile = " + outFile.getAbsolutePath());
                        Bukkit.getLogger().info("DEBUG: outFile name = " + outFile.getName());
                    }
                    if (!outFile.exists() || replace) {
                        java.nio.file.Files.copy(in, outFile.toPath());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the file that contains this addon
     * @param f the file to set
     */
    public void setAddonFile(File f) {
        this.file = f;
    }

    /**
     * Set this addon's data folder
     * @param file
     */
    public void setDataFolder(File file) {
        this.dataFolder = file;    
    }

    /**
     * Set this addons description
     * @param desc
     */
    public void setDescription(AddonDescription desc){
        this.description = desc;
    }

    /**
     * Set whether this addon is enabled or not
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
