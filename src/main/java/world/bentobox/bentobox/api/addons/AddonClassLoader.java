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
 * Loads addons and sets up permissions
 * 
 * @author Tastybento, ComminQ
 */
public class AddonClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final Addon addon;
    private final AddonsManager loader;

    /**
     * For testing only
     * 
     * @param addon   addon
     * @param loader  Addons Manager
     * @param jarFile Jar File
     * @throws MalformedURLException exception
     */
    protected AddonClassLoader(Addon addon, AddonsManager loader, File jarFile) throws MalformedURLException {
        super(new URL[] { jarFile.toURI().toURL() });
        this.addon = addon;
        this.loader = loader;
    }

    public AddonClassLoader(AddonsManager addonsManager, YamlConfiguration data, File jarFile, ClassLoader parent)
            throws InvalidAddonInheritException, MalformedURLException, InvalidDescriptionException,
            InvalidAddonDescriptionException, InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        super(new URL[] { jarFile.toURI().toURL() }, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
            if (mainClass == null) {
                throw new InvalidAddonFormatException("addon.yml does not define a main class!");
            }
            javaClass = Class.forName(mainClass, true, this);
            if (mainClass.startsWith("world.bentobox.bentobox")) {
                throw new InvalidAddonFormatException(
                        "Package declaration cannot start with 'world.bentobox.bentobox'");
            }
        } catch (Exception e) {
            throw new InvalidDescriptionException("Could not load '" + jarFile.getName() + "' in folder '"
                    + jarFile.getParent() + "' - " + e.getMessage());
        }

        Class<? extends Addon> addonClass;
        try {
            addonClass = javaClass.asSubclass(Addon.class);
        } catch (ClassCastException e) {
            throw new InvalidAddonInheritException("Main class does not extend 'Addon'");
        }

        addon = addonClass.getDeclaredConstructor().newInstance();
        addon.setDescription(asDescription(data));
    }

    /**
     * Converts the addon.yml to an AddonDescription
     * 
     * @param data - yaml config (addon.yml)
     * @return Addon Description
     * @throws InvalidAddonDescriptionException - if there's a bug in the addon.yml
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
            throw new InvalidAddonDescriptionException(
                    "Missing 'authors' tag. At least one author must be listed in addon.yml");
        }

        AddonDescription.Builder builder = new AddonDescription.Builder(
                // Mandatory elements
                Objects.requireNonNull(data.getString("main")), Objects.requireNonNull(data.getString("name")),
                Objects.requireNonNull(data.getString("version")))
                .authors(Objects.requireNonNull(data.getString("authors")))
                // Optional elements
                .metrics(data.getBoolean("metrics", true)).repository(data.getString("repository", ""));

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
            throw new InvalidAddonDescriptionException(
                    "'icon' tag refers to an unknown Material: " + data.getString("icon"));
        }
        builder.icon(Objects.requireNonNull(icon));

        String apiVersion = data.getString("api-version");
        if (apiVersion != null) {
            if (!apiVersion.replace("-SNAPSHOT", "").matches("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")) {
                throw new InvalidAddonDescriptionException("Provided API version '" + apiVersion
                        + "' is not valid. It must only contain digits and dots and not end with a dot.");
            }
            if (apiVersion.contains("-SNAPSHOT")) {
                BentoBox.getInstance().logWarning(data.getString("name")
                        + " addon depends on development version of BentoBox plugin. Some functions may be not implemented.");
            }
            builder.apiVersion(apiVersion);
        }
        // Set permissions
        if (data.isConfigurationSection("permissions")) {
            builder.permissions(Objects.requireNonNull(data.getConfigurationSection("permissions")));
        }

        return builder.build();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    @Nullable
    protected Class<?> findClass(String name) {
        return findClass(name, true);
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     * 
     * @param name        - class name
     * @param checkGlobal - check globally or not when searching
     * @return Class - class if found
     */
    public Class<?> findClass(String name, boolean checkGlobal) {
        if (name.startsWith("world.bentobox.bentobox")) {
            return null;
        }
        Class<?> result = classes.get(name);
        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                try {
                    result = super.findClass(name);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Do nothing.
                }
                if (result != null) {
                    loader.setClass(name, result);

                }
            }
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
     * @return class list
     */
    public Set<String> getClasses() {
        return classes.keySet();
    }
}
