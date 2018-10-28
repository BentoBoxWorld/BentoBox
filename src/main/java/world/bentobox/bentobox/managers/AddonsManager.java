package world.bentobox.bentobox.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonClassLoader;
import world.bentobox.bentobox.api.addons.exception.InvalidAddonFormatException;
import world.bentobox.bentobox.api.events.addon.AddonEvent;

/**
 * @author Tastybento, ComminQ
 */
public class AddonsManager {

    private static final String LOCALE_FOLDER = "locales";
    private List<Addon> addons;
    private List<AddonClassLoader> loaders;
    private final Map<String, Class<?>> classes = new HashMap<>();
    private BentoBox plugin;

    public AddonsManager(BentoBox plugin) {
        this.plugin = plugin;
        addons = new ArrayList<>();
        loaders = new ArrayList<>();
    }

    /**
     * Loads all the addons from the addons folder
     */
    public void loadAddons() {
        plugin.log("Loading addons...");
        File f = new File(plugin.getDataFolder(), "addons");
        if (!f.exists() && !f.mkdirs()) {
            plugin.logError("Cannot create addons folder!");
            return;
        }
        Arrays.stream(Objects.requireNonNull(f.listFiles())).filter(x -> !x.isDirectory() && x.getName().endsWith(".jar")).forEach(this::loadAddon);
        plugin.log("Loaded " + addons.size() + " addons.");
        sortAddons();
    }

    /**
     * Enables all the addons
     */
    public void enableAddons() {
        plugin.log("Enabling addons...");
        addons.forEach(addon -> {
            try {
                addon.onEnable();
                Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.ENABLE).build());
                addon.setState(Addon.State.ENABLED);
                plugin.log("Enabling " + addon.getDescription().getName() + "...");
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                // Looks like the addon is outdated, because it tries to refer to missing classes.
                // Set the AddonState as "INCOMPATIBLE".
                addon.setState(Addon.State.INCOMPATIBLE);
                plugin.log("Skipping " + addon.getDescription().getName() + " as it is incompatible with the current version of BentoBox or of server software...");
                plugin.log("NOTE: The addon is referring to no longer existing classes.");
                plugin.log("NOTE: DO NOT report this as a bug from BentoBox.");
            } catch (Exception | Error e) {
                // Unhandled exception. We'll give a bit of debug here.
                // Set the AddonState as "ERROR".
                addon.setState(Addon.State.ERROR);
                plugin.log("Skipping " + addon.getDescription().getName() + " due to an unhandled exception...");
                plugin.log("STACKTRACE: " + e.getClass().getSimpleName() + " - " + e.getMessage() + " - " + e.getCause());
            }
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

    private YamlConfiguration addonDescription(JarFile jar) throws InvalidAddonFormatException, IOException, InvalidConfigurationException {
        // Obtain the addon.yml file
        JarEntry entry = jar.getJarEntry("addon.yml");
        if (entry == null) {
            throw new InvalidAddonFormatException("Addon '" + jar.getName() + "' doesn't contains addon.yml file");
        }
        // Open a reader to the jar
        BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
        // Grab the description in the addon.yml file
        YamlConfiguration data = new YamlConfiguration();
        data.load(reader);
        return data;
    }

    private void loadAddon(File f) {
        Addon addon;
        try (JarFile jar = new JarFile(f)) {
            // Get description in the addon.yml file
            YamlConfiguration data = addonDescription(jar);
            // Load the addon
            AddonClassLoader addonClassLoader = new AddonClassLoader(this, data, f, this.getClass().getClassLoader());
            // Add to the list of loaders
            loaders.add(addonClassLoader);

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
            plugin.getLocalesManager().loadLocalesFromFile(addon.getDescription().getName());
            // Fire the load event
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.LOAD).build());
            // Add it to the list of addons
            addons.add(addon);
            // Run the onLoad.
            addon.onLoad();
        } catch (Exception e) {
            plugin.log(e.getMessage());
        }
    }

    /**
     * Disable all the enabled addons
     */
    public void disableAddons() {
        plugin.log("Disabling addons...");
        // Unload addons
        addons.forEach(addon -> {
            if (addon.isEnabled()) {
                addon.onDisable();
                Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.DISABLE).build());
                plugin.log("Disabling " + addon.getDescription().getName() + "...");
            }
        });

        loaders.forEach(l -> {
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

    public List<AddonClassLoader> getLoaders() {
        return loaders;
    }

    public void setLoaders(List<AddonClassLoader> loaders) {
        this.loaders = loaders;
    }

    /**
     * Finds a class by name that has been loaded by this loader
     * @param name - name of the class
     * @return Class - the class
     */
    public Class<?> getClassByName(final String name) {
        return classes.getOrDefault(name, loaders.stream().map(l -> l.findClass(name, false)).filter(Objects::nonNull).findFirst().orElse(null));
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

    private void sortAddons() {
        // Lists all available addons as names.
        List<String> names = addons.stream().map(a -> a.getDescription().getName()).collect(Collectors.toList());

        // Check that any dependencies exist
        Iterator<Addon> addonsIterator = addons.iterator();
        while (addonsIterator.hasNext()) {
            Addon a = addonsIterator.next();
            for (String dependency : a.getDescription().getDependencies()) {
                if (!names.contains(dependency)) {
                    plugin.logError(a.getDescription().getName() + " has dependency on " + dependency + " that does not exist. Addon will not load!");
                    addonsIterator.remove();
                    break;
                }
            }
        }

        // Load dependencies or soft dependencies
        Map<String,Addon> sortedAddons = new LinkedHashMap<>();
        // Start with nodes with no dependencies
        addons.stream().filter(a -> a.getDescription().getDependencies().isEmpty() && a.getDescription().getSoftDependencies().isEmpty())
        .forEach(a -> sortedAddons.put(a.getDescription().getName(), a));
        // Fill remaining
        List<Addon> remaining = addons.stream().filter(a -> !sortedAddons.containsKey(a.getDescription().getName())).collect(Collectors.toList());

        // Run through remaining addons
        remaining.forEach(addon -> {
            // Get the addon's dependencies.
            List<String> dependencies = new ArrayList<>(addon.getDescription().getDependencies());
            dependencies.addAll(addon.getDescription().getSoftDependencies());

            // Remove already sorted addons (dependencies) from the list
            dependencies.removeIf(sortedAddons::containsKey);

            if (dependencies.stream().noneMatch(dependency -> addon.getDescription().getDependencies().contains(dependency))) {
                sortedAddons.put(addon.getDescription().getName(), addon);
            }
        });

        addons.clear();
        addons.addAll(sortedAddons.values());
    }
}
