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
import us.tastybento.bskyblock.api.addons.AddOn;
import us.tastybento.bskyblock.api.addons.AddOnClassLoader;
import us.tastybento.bskyblock.api.addons.event.AddOnEnableEvent;
import us.tastybento.bskyblock.api.addons.event.AddOnLoadEvent;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddOnFormatException;
import us.tastybento.bskyblock.api.addons.exception.InvalidAddOnInheritException;

public class AddOnManager {

    private static final boolean DEBUG = false;
    private List<AddOn> addons;
    private List<AddOnClassLoader> loader;

    public AddOnManager() {
        this.addons = new ArrayList<>();
        this.loader = new ArrayList<>();
    }

    /**
     * Loads all the addons from the addons folder
     */
    public void loadAddons() {
        File f = new File(BSkyBlock.getInstance().getDataFolder(), "addons");
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File file : f.listFiles()) {
                    if (!file.isDirectory()) {
                        try {
                            this.loadAddon(file);
                        } catch (InvalidAddOnFormatException e) {
                            e.printStackTrace();
                        } catch (InvalidAddOnInheritException e) {
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
            BSkyBlock.getInstance().getServer().getPluginManager().callEvent(new AddOnEnableEvent(addon));
            addon.setEnabled(true);
            BSkyBlock.getInstance().getLogger().info("Enabling " + addon.getDescription().getName() + "...");
        });

    }


    public AddOn getAddonByName(String name){
        if(name.equals("")) return null;

        for(AddOn m  : this.addons){
            if(m.getDescription().getName().contains(name)) return m;
        }
        return null;
    }

    private void loadAddon(File f) throws InvalidAddOnFormatException, InvalidAddOnInheritException {
        try {
            AddOn addon = null;

            if (!f.getName().contains(".jar")) {
                return;
            }
            JarFile jar = new JarFile(f);
            JarEntry entry = jar.getJarEntry("addon.yml");

            if (entry == null) {
                jar.close();
                throw new InvalidAddOnFormatException("Addon doesn't contains description file");

            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));

            Map<String, String> data = this.data(reader);

            AddOnClassLoader loader = null;

            loader = new AddOnClassLoader(data, f, reader, this.getClass().getClassLoader());
            this.loader.add(loader);
            addon = loader.addon;
            addon.setDataFolder(new File(f.getParent(), f.getName().replace(".jar", "")));
            addon.setFile(f);
            AddOnLoadEvent event = new AddOnLoadEvent(addon);
            Bukkit.getPluginManager().callEvent(event);
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

    public List<AddOn> getAddons() {
        return addons;
    }

    public List<AddOnClassLoader> getLoader() {
        return loader;
    }

    public void setLoader(List<AddOnClassLoader> loader) {
        this.loader = loader;
    }

}
