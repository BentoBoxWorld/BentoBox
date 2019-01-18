package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.util.permissions.DefaultPermissions;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonFormatException;
import world.bentobox.bentobox.api.addons.exceptions.InvalidAddonInheritException;
import world.bentobox.bentobox.managers.AddonsManager;

/**
 * Loads addons and sets up permissions
 * @author Tastybento, ComminQ
 */
public class AddonClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private Addon addon;
    private AddonsManager loader;

    public AddonClassLoader(AddonsManager addonsManager, YamlConfiguration data, File path, ClassLoader parent)
            throws InvalidAddonInheritException,
            MalformedURLException,
            InvalidDescriptionException,
            InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        super(new URL[]{path.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
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
                if (perms.contains(perm + ".default") && perms.contains(perm + ".description")) {
                    registerPermission(perms, perm);
                }
            });
        }
    }

    private void registerPermission(ConfigurationSection perms, String perm) {
        PermissionDefault pd = PermissionDefault.getByName(perms.getString(perm + ".default"));
        if (pd == null) {
            Bukkit.getLogger().severe("Permission default is invalid : " + perms.getName());
            return;
        }
        String desc = perms.getString(perm + ".description");
        DefaultPermissions.registerPermission(perm, desc, pd);
    }

    private AddonDescription asDescription(YamlConfiguration data) {
        AddonDescription.Builder builder = new AddonDescription.Builder(data.getString("main"), data.getString("name"))
                .version(data.getString("version"))
                .authors(data.getString("authors"));

        if (data.getString("depend") != null) {
            builder.dependencies(Arrays.asList(data.getString("depend").split("\\s*,\\s*")));
        }
        if (data.getString("softdepend") != null) {
            builder.softDependencies(Arrays.asList(data.getString("softdepend").split("\\s*,\\s*")));
        }

        return builder.build();
    }

    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) {
        return findClass(name, true);
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     * @param name class name
     * @param checkGlobal check globally or not when searching
     * @return class if found, null otherwise
     */
    @Nullable
    public Class<?> findClass(@NonNull String name, boolean checkGlobal) {
        if (name.startsWith("world.bentobox.bentobox")) {
            return null;
        }

        // Either return the value if it exists (and != null), or try to compute one.
        // If the computed value is null, it won't be added to the map.
        return classes.computeIfAbsent(name, (key) -> {
            Class<?> computed = checkGlobal ? loader.getClassByName(key) : null;

            if (computed == null) {
                try {
                    computed = super.findClass(key);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    computed = null;
                }
                if (computed != null) {
                    loader.setClass(key, computed);
                }
            }
            return computed;
        });
    }

    /**
     * @return the addon
     */
    public Addon getAddon() {
        return addon;
    }
}
