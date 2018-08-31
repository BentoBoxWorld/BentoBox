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

import world.bentobox.bentobox.api.addons.AddonDescription.AddonDescriptionBuilder;
import world.bentobox.bentobox.api.addons.exception.InvalidAddonFormatException;
import world.bentobox.bentobox.api.addons.exception.InvalidAddonInheritException;
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
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        super(new URL[]{path.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass;
        try {
            String mainClass = data.getString("main");
            javaClass = Class.forName(mainClass, true, this);
            if(mainClass.startsWith("world.bentobox.bentobox")){
                throw new InvalidAddonFormatException("Packages declaration cannot start with 'world.bentobox.bentobox'");
            }
        } catch (Exception e) {
            throw new InvalidDescriptionException("Could not load '" + path.getName() + "' in folder '" + path.getParent() + "' - " + e.getMessage());
        }

        Class<? extends Addon> addonClass;
        try{
            addonClass = javaClass.asSubclass(Addon.class);
        } catch(ClassCastException e){
            throw new InvalidAddonInheritException("Main class doesn't not extends super class 'Addon'");
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
        AddonDescriptionBuilder adb = new AddonDescriptionBuilder(data.getString("name"))
                .withVersion(data.getString("version"))
                .withAuthor(data.getString("authors"));
        if (data.getString("depend") != null) {
            adb.withDepend(Arrays.asList(data.getString("depend").split("\\s*,\\s*")));
        }
        if (data.getString("softdepend") != null) {
            adb.withSoftDepend(Arrays.asList(data.getString("softdepend").split("\\s*,\\s*")));
        }
        return adb.build();
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
                } catch (ClassNotFoundException e) {
                    result = null;
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

}
