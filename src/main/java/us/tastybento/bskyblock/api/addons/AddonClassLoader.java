package us.tastybento.bskyblock.api.addons;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.util.permissions.DefaultPermissions;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.addons.AddonDescription.AddonDescriptionBuilder;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddonInheritException;
import us.tastybento.bskyblock.managers.AddonsManager;

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
            InvalidAddonFormatException,
            InvalidDescriptionException,
            InstantiationException,
            IllegalAccessException {
        super(new URL[]{path.toURI().toURL()}, parent);

        loader = addonsManager;

        Class<?> javaClass = null;
        try {
            String mainClass = data.getString("main");
            javaClass = Class.forName(mainClass, true, this);
            if(mainClass.contains("us.tastybento")){
                throw new InvalidAddonFormatException("Packages declaration cannot start with 'us.tastybento'");
            }
        } catch (ClassNotFoundException e) {
            BSkyBlock.getInstance().logError("Could not load '" + path.getName() + "' in folder '" + path.getParent() + "' - invalid addon.yml");
            throw new InvalidDescriptionException("Invalid addon.yml");
        }

        Class<? extends Addon> addonClass;
        try{
            addonClass = javaClass.asSubclass(Addon.class);
        } catch(ClassCastException e){
            throw new InvalidAddonInheritException("Main class doesn't not extends super class 'Addon'");
        }

        addon = addonClass.newInstance();
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

    private AddonDescription asDescription(YamlConfiguration data){
        return new AddonDescriptionBuilder(data.getString("name"))
                .withVersion(data.getString("version"))
                .withAuthor(data.getString("authors")).build();
    }


    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     * @param name
     * @param checkGlobal
     * @return Class
     */
    public Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("us.tastybento.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);
        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }
            if (result == null) {
                result = super.findClass(name);
                if (result != null) {
                    loader.setClass(name, result);
                }
                classes.put(name, result);
            }
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
