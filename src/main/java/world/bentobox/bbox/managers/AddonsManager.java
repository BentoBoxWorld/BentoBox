package world.bentobox.bbox.managers;

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
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.api.addons.Addon;
import world.bentobox.bbox.api.addons.AddonClassLoader;
import world.bentobox.bbox.api.addons.exception.InvalidAddonFormatException;
import world.bentobox.bbox.api.events.addon.AddonEvent;

/**
 * @author Tastybento, ComminQ
 */
public class AddonsManager {

    private static final boolean DEBUG = false;
    private static final String LOCALE_FOLDER = "locales";
    private List<Addon> addons;
    private List<AddonClassLoader> loader;
    private final Map<String, Class<?>> classes = new HashMap<>();
    private BentoBox plugin;

    public AddonsManager(BentoBox plugin) {
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
        if (!f.exists() && !f.mkdirs()) {
            plugin.logError("Cannot make addons folder!");
            return;
        }

        Arrays.stream(Objects.requireNonNull(f.listFiles())).filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(this::loadAddon);
        addons.forEach(Addon::onLoad);
        plugin.log("Loaded " + addons.size() + " addons.");
    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        plugin.log("Enabling addons...");
        addons.forEach(addon -> {
            addon.onEnable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build());
            addon.setEnabled(true);
            plugin.log("Enabling " + addon.getDescription().getName() + "...");
        });
        plugin.log("Addons successfully enabled.");
    }

    /**
     * Gets the addon by name
     * @param name - addon name
     * @return Optional addon object
     */
    public Optional<Addon> getAddonByName(String name){
        return addons.stream().filter(a -> a.getDescription().getName().contains(name)).findFirst();
    }

    private void loadAddon(File f) {
        try {
            Addon addon;
            // Check that this is a jar
            if (!f.getName().endsWith(".jar")) {
                throw new IOException("Filename must end in .jar. Name is '" + f.getName() + "'");
            }
            try (JarFile jar = new JarFile(f)) {
                // Obtain the addon.yml file
                JarEntry entry = jar.getJarEntry("addon.yml");
                if (entry == null) {
                    throw new InvalidAddonFormatException("Addon '" + f.getName() + "' doesn't contains addon.yml file");
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
                plugin.log("Loaded BSkyBlock addon " + addon.getDescription().getName() + "...");
            } catch (Exception e) {
                plugin.log(e.getMessage());
            }

        } catch (Exception e) {
            if (DEBUG) {
                plugin.log(f.getName() + " is not a jarfile, ignoring...");
            }
        }
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        plugin.log("Disabling addons...");
        // Unload addons
        addons.forEach(addon -> {
            addon.onDisable();
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build());
            plugin.log("Disabling " + addon.getDescription().getName() + "...");
        });

        loader.forEach(l -> {
            try {
                l.close();
            } catch (IOException ignore) {
                // Ignore
            }
        });
        plugin.log("Addons successfully disabled.");
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
     * @param name - name of the class
     * @return Class - the class
     */
    public Class<?> getClassByName(final String name) {
        return classes.getOrDefault(name, loader.stream().map(l -> l.findClass(name, false)).filter(Objects::nonNull).findFirst().orElse(null));
    }

    /**
     * Sets a class that this loader should know about
     *
     * @param name - name of the class
     * @param clazz - the class
     */
    public void setClass(final String name, final Class<?> clazz) {
        classes.putIfAbsent(name, clazz);
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

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();

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
