package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.util.permissions.DefaultPermissions;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonDescriptionException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonFormatException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonInheritException;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * Loads addons and sets up permissions
 * @author Tastybento, ComminQ
 */
public class AddonClassLoader extends URLClassLoader {

    private static final String DEFAULT = ".default";
    private final Map<String, Class<?>> classes = new HashMap<>();
    private Addon addon;
    private AddonsManager loader;

    public AddonClassLoader(AddonsManager addonsManager, YamlConfiguration data, File path, ClassLoader parent)
            throws InvalidAddonInheritException,
            MalformedURLException,
            InvalidDescriptionException,
            InvalidAddonDescriptionException,
            InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        super(new URL[]{path.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
            if (mainClass == null) {
                throw new InvalidAddonFormatException("addon.yml does not define a main class!");
            }
            javaClass = Class.forName(mainClass, true, this);
            if(mainClass.startsWith("world.bentobox.bentobox")){
                throw new InvalidAddonFormatException("Package declaration cannot start with 'world.bentobox.bentobox'");
            }
        } catch (Exception e) {
            throw new InvalidDescriptionException("Could not load '" + path.getName() + "' in folder '" + path.getParent() + "' - " + e.getMessage());
        }

        Class<? extends Addon> addonClass;
        try {
            addonClass = javaClass.asSubclass(Addon.class);
        } catch (ClassCastException e) {
            throw new InvalidAddonInheritException("Main class does not extend 'Addon'");
        }

        addon = addonClass.getDeclaredConstructor().newInstance();
        addon.setDescription(asDescription(data));
        // Set permissions
        if (data.isConfigurationSection("permissions")) {
            ConfigurationSection perms = data.getConfigurationSection("permissions");
            perms.getKeys(true).forEach(perm -> {
                if (perms.contains(perm + DEFAULT) && perms.contains(perm + ".description")) {
                    registerPermission(perms, perm);
                }
            });
        }
    }

    private void registerPermission(ConfigurationSection perms, String perm) {
        if (perms.getString(perm + DEFAULT) == null) {
            Bukkit.getLogger().severe("Permission default is invalid : " + perms.getName());
            return;
        }
        PermissionDefault pd = PermissionDefault.getByName(perms.getString(perm + DEFAULT));
        if (pd == null) {
            Bukkit.getLogger().severe("Permission default is invalid : " + perms.getName());
            return;
        }
        String desc = perms.getString(perm + ".description");
        DefaultPermissions.registerPermission(perm, desc, pd);
    }

    @NonNull
    private AddonDescription asDescription(YamlConfiguration data) throws InvalidAddonDescriptionException {
        AddonDescription.Builder builder = new AddonDescription.Builder(data.getString("main"), data.getString("name"), data.getString("version"))
                .authors(data.getString("authors"))
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
        builder.icon(Material.getMaterial(data.getString("icon", "PAPER")));

        String apiVersion = data.getString("api-version");
        if (apiVersion != null) {
            if (!apiVersion.matches("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")) {
                throw new InvalidAddonDescriptionException("Provided API version '" + apiVersion + "' is not valid. It must only contain digits and dots and not end with a dot.");
            }
            builder.apiVersion(apiVersion);
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
     * This is a custom findClass that enables classes in other addons to be found
     * @param name - class name
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
