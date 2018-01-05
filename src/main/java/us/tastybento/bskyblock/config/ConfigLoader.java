package us.tastybento.bskyblock.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.Settings.GameType;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.ConfigEntry.NoAdapter;

public class ConfigLoader {

    public ConfigLoader() {
        try {
            getAnnotations(Settings.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void getAnnotations(Class<Settings> clazz) throws IllegalArgumentException, IllegalAccessException, NoSuchMethodException, SecurityException, InvocationTargetException, InstantiationException{
        for(Field field : clazz.getDeclaredFields()){
            Class<?> type = field.getType();
            String name = field.getName();
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            
            
            // If there is a config annotation then do something
            if (configEntry != null) {
                // Get the current value
                Object currentvalue = field.get(null);
                // Get the setting as defined in the config file
                Object configValue = BSkyBlock.getInstance().getConfig().get(configEntry.path());
                // If this setting is for use in this game
                if (configEntry.specificTo().equals(GameType.BOTH) || configEntry.specificTo().equals(Settings.GAMETYPE)) {
                    // If there's a difference in the value. Note for non-primitives, this will be true
                    if (!currentvalue.equals(configValue)) {
                        Bukkit.getLogger().info(name + " changed value from " + currentvalue + " to " + configValue);
                        // If there's an adapter use it to convert from the YML object to the setting field
                        if (!configEntry.adapter().equals(NoAdapter.class)) {
                            // Create an instance of the adapter class
                            Object instance = configEntry.adapter().newInstance();
                            // Get the adapt method - it should be there.
                            Method method = configEntry.adapter().getMethod("adapt", Object.class, Object.class);
                            if (method != null) {
                                // It exists, so invoke it
                                configValue = method.invoke(instance, configValue, type);
                                if (configValue != null) {
                                    // Set the field.
                                    field.set(null, configValue);
                                }
                            }
                        } else if (configValue != null){
                            // No adapter - I hope this works!
                            field.set(null, configValue);
                        }
                    }
                } else {
                    Bukkit.getLogger().info(name + " not applicable to this game type");
                }
            }
        }
    }
}
