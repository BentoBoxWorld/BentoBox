package us.tastybento.bskyblock.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
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
public final class AddonsManager {

    private static final boolean DEBUG = false;
    private List<Addon> addons;
    private List<AddonClassLoader> loader;
    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();



    public AddonsManager() {
        this.addons = new ArrayList<>();
        this.loader = new ArrayList<>();
    }

    /**
     * Loads all the addons from the addons folder
     * @throws InvalidDescriptionException 
     */
    public void enableAddons() {
        File f = new File(BSkyBlock.getInstance().getDataFolder(), "addons");
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File file : f.listFiles()) {
                    if (!file.isDirectory()) {
                        try {
                            this.loadAddon(file);
                        } catch (InvalidAddonFormatException e) {
                            e.printStackTrace();
                        } catch (InvalidAddonInheritException e) {
                            e.printStackTrace();
                        } catch (InvalidDescriptionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            try {
                f.mkdir();
            } catch (SecurityException e) {
                e.printStackTrace();
                if (DEBUG) {
                    Bukkit.getLogger().severe("Cannot create folder 'addons' (Permission ?)");
                }
            }
        }

        this.addons.stream().forEach(addon -> {
            addon.onEnable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build());
            addon.setEnabled(true);
            BSkyBlock.getInstance().getLogger().info("Enabling " + addon.getDescription().getName() + "...");
        });

    }

    /**
     * Gets the addon by name
     * @param name
     * @return
     */
    public Optional<Addon> getAddonByName(String name){
        if(name.equals("")) return Optional.empty();

        for(Addon addon  : this.addons){
            if(addon.getDescription().getName().contains(name)) return Optional.of(addon);
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
            JarFile jar = new JarFile(f);
            
            // Obtain the addon.yml file
            JarEntry entry = jar.getJarEntry("addon.yml");
            if (entry == null) {
                jar.close();
                throw new InvalidAddonFormatException("Addon doesn't contains description file");

            }
            // Open a reader to the jar
            BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
            // Grab the description in the addon.yml file
            Map<String, String> data = this.data(reader);

            // Load the addon
            AddonClassLoader loader = new AddonClassLoader(this, data, f, reader, this.getClass().getClassLoader());
            // Add to the list of loaders
            this.loader.add(loader);
            
            // Get the addon itseld
            addon = loader.addon;
            // Initialize some settings
            addon.setDataFolder(new File(f.getParent(), addon.getDescription().getName()));
            addon.setAddonFile(f);
            
            // Fire the load event
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.LOAD).build());
            
            // Add it to the list of addons
            this.addons.add(addon);
            
            // Run the onLoad() method
            addon.onLoad();
            
            // Inform the console
            BSkyBlock.getInstance().getLogger().info("Loading BSkyBlock addon " + addon.getDescription().getName() + "...");
            
            // Close the jar
            jar.close();
        } catch (IOException e) {
            if (DEBUG) {
                BSkyBlock.getInstance().getLogger().info(f.getName() + "is not a jarfile, ignoring...");
            }
        }

    }

    private Map<String, String> data(BufferedReader reader) {
        Map<String, String> map = new HashMap<>();
        reader.lines().forEach(string -> {
            if (DEBUG)
                Bukkit.getLogger().info("DEBUG: " + string);
            String[] data = string.split("\\: ");
            if (data.length > 1) {
                map.put(data[0], data[1].substring(0, data[1].length()));
            }
        });
        return map;
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        // Unload addons
        addons.forEach(addon -> {
            addon.onDisable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build());
            System.out.println("Disabling " + addon.getDescription().getName() + "...");
        });

        loader.forEach(loader -> {
            try {
                loader.close();
            } catch (IOException e) {
                e.printStackTrace();
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
     * @param name
     * @return Class
     */
    public Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (AddonClassLoader loader : loader) {
                try {
                    cachedClass = loader.findClass(name, false);
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
     * @param name
     * @param clazz
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

}
