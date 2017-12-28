package us.tastybento.bskyblock.api.addons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Add-on class for BSkyBlock. Extend this to create an add-on.
 * The operation and methods are very similar to Bukkit's JavaPlugin.
 * @author ben, comminq
 *
 */
public abstract class AddOn implements AddOnInterface  {

    private static final String ADDON_CONFIG_FILENAME = "config.yml";
    private boolean enabled;
    private AddOnDescription description;
    private FileConfiguration config;
    private File dataFolder;
    private File file;

    public AddOn() {
        this.enabled = false;
    }

    public JavaPlugin getBSkyBlock(){
        return BSkyBlock.getInstance();
    }

    public FileConfiguration getConfig() {
        config = loadYamlFile(ADDON_CONFIG_FILENAME);
        return config;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public AddOnDescription getDescription() {
        return description;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    public Logger getLogger() {
        return getBSkyBlock().getLogger();
    }

    public Server getServer() {
        return getBSkyBlock().getServer();
    }

    public boolean isEnabled() {
        return enabled;
    }

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

    public void registerListener(Listener listener){
        BSkyBlock.getInstance().getServer().getPluginManager().registerEvents(listener, BSkyBlock.getInstance());
    }

    public void saveDefaultConfig() {
        saveResource(ADDON_CONFIG_FILENAME, true);
        config = loadYamlFile(ADDON_CONFIG_FILENAME);
    }

    /**
     * Saves a resource contained in this add-on's jar file.
     * @param resourcePath
     * @param replace
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = null; 
        try {
            JarFile jar = new JarFile(file);
            JarEntry config = jar.getJarEntry(resourcePath);
            if (config != null) {
                in = jar.getInputStream(config);
            }
            if (in == null) {
                jar.close();
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + jar.getName());
            }
            File outFile = new File(dataFolder, resourcePath);
            int lastIndex = resourcePath.lastIndexOf('/');
            File outDir = new File(dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

            if (!outDir.exists()) {
                outDir.mkdirs();
            }


            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                getLogger().warning("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
            jar.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setDataFolder(File file) {
        this.dataFolder = file;    
    }

    public void setDescription(AddOnDescription desc){
        this.description = desc;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param f the file to set
     */
    public void setFile(File f) {
        this.file = f;
    }
    
}
