package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonDescriptionException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonFormatException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonInheritException;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * This class loader is responsible for loading an addon's JAR file, its classes,
 * and its {@code addon.yml} metadata.
 * <p>
 * Each addon is loaded with its own {@link AddonClassLoader}, which allows for
 * class isolation and management. This loader also facilitates inter-addon
 * class sharing by coordinating with the {@link AddonsManager}.
 * <p>
 * This approach is now rarely used as most addons are now Plugin-based and so are loaded by the server as plugins.
 *
 * @author tastybento, ComminQ
 * @since 1.0
 */
public class AddonClassLoader extends URLClassLoader {

    /**
     * A cache of classes that have been loaded by this class loader.
     */
    private final Map<String, Class<?>> classes = new HashMap<>();
    /**
     * The addon instance that was loaded by this class loader.
     */
    private final Addon addon;
    /**
     * The addon manager that created this class loader.
     */
    private final AddonsManager loader;

    /**
     * For testing only
     * @param addon addon
     * @param loader Addons Manager
     * @param jarFile Jar File
     * @throws MalformedURLException exception
     */
    protected AddonClassLoader(Addon addon, AddonsManager loader, File jarFile) throws MalformedURLException {
        super(new URL[]{jarFile.toURI().toURL()});
        this.addon = addon;
        this.loader = loader;
    }

    /**
     * Constructs a new AddonClassLoader for a given addon.
     *
     * @param addonsManager The addon manager instance.
     * @param data The addon's metadata from {@code addon.yml}.
     * @param jarFile The addon's JAR file.
     * @param parent The parent class loader.
     * @throws InvalidAddonInheritException If the main class does not extend {@link Addon}.
     * @throws MalformedURLException If the JAR file path is invalid.
     * @throws InvalidDescriptionException If the addon cannot be loaded.
     * @throws InvalidAddonDescriptionException If the {@code addon.yml} is invalid.
     * @throws InstantiationException If the addon's main class cannot be instantiated.
     * @throws IllegalAccessException If the addon's main class constructor is not accessible.
     * @throws InvocationTargetException If the addon's constructor throws an exception.
     * @throws NoSuchMethodException If the addon's main class has no default constructor.
     */
    public AddonClassLoader(AddonsManager addonsManager, YamlConfiguration data, File jarFile, ClassLoader parent)
            throws InvalidAddonInheritException,
            MalformedURLException,
            InvalidDescriptionException,
            InvalidAddonDescriptionException,
            InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        super(new URL[]{jarFile.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
            if (mainClass == null) {
                throw new InvalidAddonFormatException("addon.yml does not define a main class!");
            }
            // Do not allow addons to use BentoBox's package.
            if(mainClass.startsWith("world.bentobox.bentobox")){
                throw new InvalidAddonFormatException("Package declaration cannot start with 'world.bentobox.bentobox'");
            }
            javaClass = Class.forName(mainClass, true, this);
        } catch (Exception e) {
            throw new InvalidDescriptionException("Could not load '" + jarFile.getName() + "' in folder '" + jarFile.getParent() + "' - " + e.getMessage());
        }

        Class<? extends Addon> addonClass;
        try {
            // Check if the main class extends Addon.
            addonClass = javaClass.asSubclass(Addon.class);
        } catch (ClassCastException e) {
            throw new InvalidAddonInheritException("Main class does not extend 'Addon'");
        }

        // Instantiate the addon and set its description from addon.yml.
        addon = addonClass.getDeclaredConstructor().newInstance();
        addon.setDescription(asDescription(data));
    }



    /**
     * Parses the addon.yml configuration into an {@link AddonDescription} object.
     *
     * @param data The YAML configuration from {@code addon.yml}.
     * @return An {@link AddonDescription} instance.
     * @throws InvalidAddonDescriptionException If the {@code addon.yml} is missing required fields or contains invalid values.
     */
    @NonNull
    public static AddonDescription asDescription(YamlConfiguration data) throws InvalidAddonDescriptionException {
        // Validate addon.yml
        if (!data.contains("main")) {
            throw new InvalidAddonDescriptionException("Missing 'main' tag. A main class must be listed in addon.yml");
        }
        if (!data.contains("name")) {
            throw new InvalidAddonDescriptionException("Missing 'name' tag. An addon name must be listed in addon.yml");
        }
        if (!data.contains("version")) {
            throw new InvalidAddonDescriptionException("Missing 'version' tag. A version must be listed in addon.yml");
        }
        if (!data.contains("authors")) {
            throw new InvalidAddonDescriptionException("Missing 'authors' tag. At least one author must be listed in addon.yml");
        }

        AddonDescription.Builder builder = new AddonDescription.Builder(
                // Mandatory elements
                Objects.requireNonNull(data.getString("main")),
                Objects.requireNonNull(data.getString("name")),
                Objects.requireNonNull(data.getString("version")))
                .authors(Objects.requireNonNull(data.getString("authors")))
                // Optional elements
                .metrics(data.getBoolean("metrics", true))
                .repository(data.getString("repository", ""));

        String depend = data.getString("depend");
        if (depend != null) {
            builder.dependencies(Arrays.asList(depend.split("\\s*,\\s*")));
        }
        String softDepend = data.getString("softdepend");
        if (softDepend != null) {
            builder.softDependencies(Arrays.asList(softDepend.split("\\s*,\\s*")));
        }
        Material icon = Material.getMaterial(data.getString("icon", "PAPER").toUpperCase(Locale.ENGLISH));
        if (icon == null) {
            throw new InvalidAddonDescriptionException("'icon' tag refers to an unknown Material: " + data.getString("icon"));
        }
        builder.icon(Objects.requireNonNull(icon));

        String apiVersion = data.getString("api-version");
        if (apiVersion != null) {
            if (!apiVersion.replace("-SNAPSHOT", "").matches("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")) {
                throw new InvalidAddonDescriptionException("Provided API version '" + apiVersion + "' is not valid. It must only contain digits and dots and not end with a dot.");
            }
            if (apiVersion.contains("-SNAPSHOT")) {
                BentoBox.getInstance().logWarning(data.getString("name") + " addon depends on development version of BentoBox plugin. Some functions may be not implemented.");
            }
            builder.apiVersion(apiVersion);
        }
        // Set permissions
        if (data.isConfigurationSection("permissions")) {
            builder.permissions(Objects.requireNonNull(data.getConfigurationSection("permissions")));
        }

        return builder.build();
    }

    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    @Nullable
    protected Class<?> findClass(String name) {
        return findClass(name, true);
    }

    /**
     * Finds and loads the specified class.
     * <p>
     * This custom implementation enables classes in one addon to be found by another.
     * The loading strategy is as follows:
     * <ol>
     *     <li>Check the local cache for this class loader.</li>
     *     <li>If not found, check the global cache in {@link AddonsManager} (if {@code checkGlobal} is true).</li>
     *     <li>If still not found, try to load the class from the addon's JAR file.</li>
     *     <li>If loaded successfully from the JAR, register it in the global cache.</li>
     *     <li>Finally, cache the class locally.</li>
     * </ol>
     *
     * @param name The fully qualified name of the class.
     * @param checkGlobal If true, check for classes loaded by other addons.
     * @return The loaded {@link Class}, or {@code null} if the class could not be found.
     */
    public Class<?> findClass(String name, boolean checkGlobal) {
        // Do not allow addons to load BentoBox classes.
        if (name.startsWith("world.bentobox.bentobox")) {
            return null;
        }
        // Check local cache first.
        Class<?> result = classes.get(name);
        if (result == null) {
            // Check global cache for classes from other addons.
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                // Try to find the class in this addon's jar.
                try {
                    result = super.findClass(name);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Do nothing. The class is not in this jar.
                }
                if (result != null) {
                    // Class found in this addon's jar, so add it to the global cache.
                    loader.setClass(name, result);

                }
            }
            // Add the class to the local cache.
            classes.put(name, result);
        }
        return result;
    }

    /**
     * @return the addon
     */
    public Addon getAddon() {
        return addon;
    }

    /**
     * Gets the set of fully qualified class names loaded by this class loader.
     * @return A set of class names.
     */
    public Set<String> getClasses() {
        return classes.keySet();
    }
}
