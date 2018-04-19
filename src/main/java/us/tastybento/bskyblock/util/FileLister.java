package us.tastybento.bskyblock.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Tastybento
 * @author Poslovitch
 */
public class FileLister{
    private Plugin plugin;

    public FileLister(Plugin level){
        plugin = level;
    }

    /**
     * Returns a list of yml files in the folder given. If the folder does not exist in the file system
     * it can check the plugin jar instead.
     * @param folderPath
     * @param checkJar - if true, the jar will be checked
     * @return List of file names
     
     */
    public List<String> list(String folderPath, boolean checkJar) throws IOException {
        List<String> result = new ArrayList<>();

        // Check if the folder exists
        File localeDir = new File(plugin.getDataFolder(), folderPath);
        if (localeDir.exists()) {
            FilenameFilter ymlFilter = (File dir, String name) -> name.toLowerCase().endsWith(".yml");
            return Arrays.asList(localeDir.list(ymlFilter));
        } else if (checkJar) {
            // Else look in the JAR
            File jarfile;

            /**
             * Get the jar file from the plugin.
             */
            try {
                Method method = JavaPlugin.class.getDeclaredMethod("getFile");
                method.setAccessible(true);

                jarfile = (File) method.invoke(plugin);
            } catch (Exception e) {
                throw new IOException(e);
            }

            JarFile jar = new JarFile(jarfile);

            /**
             * Loop through all the entries.
             */
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();

                /**
                 * Not in the folder.
                 */
                if (!path.startsWith(folderPath)) {
                    continue;
                }

                if (entry.getName().endsWith(".yml")) {
                    result.add(entry.getName());
                }

            }
            jar.close();
        }
        return result;
    }

    public List<String> listJar(String folderPath) throws IOException {
        List<String> result = new ArrayList<>();
        // Look in the JAR
        File jarfile;

        /**
         * Get the jar file from the plugin.
         */
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);

            jarfile = (File) method.invoke(plugin);
        } catch (Exception e) {
            throw new IOException(e);
        }

        JarFile jar = new JarFile(jarfile);

        /**
         * Loop through all the entries.
         */
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();

            /**
             * Not in the folder.
             */
            if (!path.startsWith(folderPath)) {
                continue;
            }

            if (entry.getName().endsWith(".yml")) {
                result.add(entry.getName());
            }

        }
        jar.close();

        return result;
    }
}
