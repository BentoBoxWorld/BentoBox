package us.tastybento.bskyblock.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;

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

    public AddonsManager() {
        this.addons = new ArrayList<>();
        this.loader = new ArrayList<>();
    }

    /**
     * Loads all the addons from the addons folder
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


    public Addon getAddonByName(String name){
        if(name.equals("")) return null;

        for(Addon m  : this.addons){
            if(m.getDescription().getName().contains(name)) return m;
        }
        return null;
    }

    private void loadAddon(File f) throws InvalidAddonFormatException, InvalidAddonInheritException {
        try {
            Addon addon = null;

            if (!f.getName().contains(".jar")) {
                return;
            }
            JarFile jar = new JarFile(f);
            JarEntry entry = jar.getJarEntry("addon.yml");

            if (entry == null) {
                jar.close();
                throw new InvalidAddonFormatException("Addon doesn't contains description file");

            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));

            Map<String, String> data = this.data(reader);

            AddonClassLoader loader = null;

            loader = new AddonClassLoader(data, f, reader, this.getClass().getClassLoader());
            this.loader.add(loader);
            addon = loader.addon;
            addon.setDataFolder(new File(f.getParent(), f.getName().replace(".jar", "")));
            addon.setAddonFile(f);
            Bukkit.getPluginManager().callEvent(AddonEvent.builder().addon(addon).reason(AddonEvent.Reason.LOAD).build());
            this.addons.add(addon);
            addon.onLoad();
            BSkyBlock.getInstance().getLogger().info("Loading BSkyBlock addon " + addon.getDescription().getName() + "...");
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

}
