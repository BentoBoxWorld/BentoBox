package us.tastybento.bskyblock.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.InvalidDescriptionException;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.addons.AddonClassLoader;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonInheritException;
import us.tastybento.bskyblock.api.events.addon.AddonEvent;

/**
 * @author Tastybento, ComminQ
 */
public class AddonsManager {

    private static final boolean DEBUG = false;
    private static final String LOCALE_FOLDER = "locales";
    private List<Addon> addons;
    private List<AddonClassLoader> loader;
    private final Map<String, Class<?>> classes = new HashMap<>();
    private BSkyBlock plugin;

    public AddonsManager(BSkyBlock plugin) {
        this.plugin = plugin;
        addons = new ArrayList<>();
        loader = new ArrayList<>();
    }

    /**
     * Loads all the addons from the addons folder
     */
    public void loadAddons() {
        plugin.log("Loading addons...");
        File f = new File(plugin.getDataFolder(), "addons");
        if (!f.exists()) {
            f.mkdirs();
        }
        Arrays.asList(f.listFiles()).stream().filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(t -> {
            plugin.log("Loading " + t.getName());
            try {
                loadAddon(t);
            } catch (Exception e) {
                plugin.logError("Could not load addon " + t.getName() + " : " + e.getMessage());
            }
        });
        addons.forEach(Addon::onLoad);
    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        addons.forEach(addon -> {
            addon.onEnable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build());
            addon.setEnabled(true);
            plugin.log("Enabling " + addon.getDescription().getName() + "...");
        });

    }

    /**
     * Gets the addon by name
     * @param name - addon name
     * @return Optional addon object
     */
    public Optional<Addon> getAddonByName(String name){
        if(name.equals("")) {
            return Optional.empty();
        }

        for(Addon addon  : addons){
            if(addon.getDescription().getName().contains(name)) {
                return Optional.of(addon);
            }
        }
        return Optional.empty();
    }

    private void loadAddon(File f) throws InvalidAddonFormatException, InvalidAddonInheritException, InvalidDescriptionException {
        try {
            Addon addon = null;
            // Check that this is a jar
            if (!f.getName().endsWith(".jar")) {
                return;
            }
            try (JarFile jar = new JarFile(f)) {

                // Obtain the addon.yml file
                JarEntry entry = jar.getJarEntry("addon.yml");
                if (entry == null) {
                    throw new InvalidAddonFormatException("Addon doesn't contains description file");

                }
                // Open a reader to the jar
                BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
                // Grab the description in the addon.yml file
                YamlConfiguration data = new YamlConfiguration();
                data.load(reader);
                // Load the addon
                AddonClassLoader addonClassLoader = new AddonClassLoader(this, data, f, this.getClass().getClassLoader());
                // Add to the list of loaders
                loader.add(addonClassLoader);

                // Get the addon itself
                addon = addonClassLoader.getAddon();
                // Initialize some settings
                addon.setDataFolder(new File(f.getParent(), addon.getDescription().getName()));
                addon.setAddonFile(f);

                File localeDir = new File(plugin.getDataFolder(), LOCALE_FOLDER + File.separator + addon.getDescription().getName());
                // Obtain any locale files and save them
                for (String localeFile : listJarYamlFiles(jar, LOCALE_FOLDER)) {
                    addon.saveResource(localeFile, localeDir, false, true);
                }
                plugin.getLocalesManager().loadLocales(addon.getDescription().getName());

                // Fire the load event
                Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.LOAD).build());

                // Add it to the list of addons
                addons.add(addon);

                // Inform the console
                plugin.log("Loading BSkyBlock addon " + addon.getDescription().getName() + "...");
                
                // Run the onLoad() method
                addon.onLoad();
            }

        } catch (Exception e) {
            if (DEBUG) {
                plugin.log(f.getName() + "is not a jarfile, ignoring...");
            }
        }

    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        // Unload addons
        addons.forEach(addon -> {
            addon.onDisable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build());
            plugin.log("Disabling " + addon.getDescription().getName() + "...");
        });

        loader.forEach(l -> {
            try {
                l.close();
            } catch (IOException e) {
                // Do nothing
            }
        });
    }

    public List<Addon> getAddons() {
        return addons;
    }

    public List<AddonClassLoader> getLoader() {
        return loader;
    }

    public void setLoader(List<AddonClassLoader> loader) {
        this.loader = loader;
    }

    /**
     * Finds a class by name that has been loaded by this loader
     * Code copied from Bukkit JavaPluginLoader
     * @param name - name of the class
     * @return Class - the class
     */
    public Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (AddonClassLoader l : loader) {
                try {
                    cachedClass = l.findClass(name, false);
                } catch (ClassNotFoundException cnfe) {}
                if (cachedClass != null) {
                    return cachedClass;
                }
            }
        }
        return null;
    }

    /**
     * Sets a class that this loader should know about
     * Code copied from Bukkit JavaPluginLoader
     *
     * @param name - name of the class
     * @param clazz - the class
     */
    public void setClass(final String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name, clazz);

            if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass(ConfigurationSerializable.class);
                ConfigurationSerialization.registerClass(serializable);
            }
        }
    }

    /**
     * Lists all the yml files found in the jar in the folder
     * @param jar - the jar file
     * @param folderPath - the path within the jar
     * @return a list of files
      - if the file cannot be read
     */
    public List<String> listJarYamlFiles(JarFile jar, String folderPath) {
        List<String> result = new ArrayList<>();

        /**
         * Loop through all the entries.
         */
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();

            /**
             * Not in the folder.
             */
            if (!path.startsWith(folderPath)) {
                continue;
            }

            if (entry.getName().endsWith(".yml")) {
                result.add(entry.getName());
            }

        }
        return result;
    }
}
