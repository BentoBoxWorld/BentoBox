package us.tastybento.bskyblock.util.placeholders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Handles hooks with other Placeholder APIs.
 * 
 * @author Poslovitch, Tastybento
 */
public class PlaceholderHandler {
    private static final String PACKAGE = "us.tastybento.bskyblock.util.placeholders.hooks.";
    /**
     * List of API classes in the package specified above (except the Internal one)
     */
    private static final String[] HOOKS = {
            //TODO
    };

    private static List<PlaceholderInterface> apis = new ArrayList<>();

    /**
     * Register placeholders and hooks
     * @param plugin
     */
    public static void register(BSkyBlock plugin){
        // Register placeholders
        new Placeholders(plugin);

        // Load Internal Placeholder API
        try{
            Class<?> clazz = Class.forName(PACKAGE + "InternalPlaceholderImpl");
            PlaceholderInterface internal = (PlaceholderInterface)clazz.newInstance();
            apis.add(internal);
        } catch (Exception e){
            // Should never happen.
            plugin.getLogger().severe("Failed to load default placeholder API");
        }

        // Load hooks
        for(String hook : HOOKS){
            if(plugin.getServer().getPluginManager().isPluginEnabled(hook)){
                try{
                    Class<?> clazz = Class.forName(PACKAGE + hook + "PlaceholderImpl");
                    PlaceholderInterface api = (PlaceholderInterface)clazz.newInstance();
                    if(api.register(plugin)){
                        plugin.getLogger().info("Hooked placeholders into " + hook);
                        apis.add(api);
                    } else {
                        plugin.getLogger().info("Failed to hook placeholders into " + hook);
                    }
                } catch (Exception e){
                    plugin.getLogger().info("Failed to hook placeholders into " + hook);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Unregister placeholder hooks
     * @param plugin
     */
    public static void unregister(BSkyBlock plugin){
        Iterator<PlaceholderInterface> it = apis.iterator();
        while (it.hasNext()) {
            PlaceholderInterface api = it.next();
            api.unregister(plugin);
            it.remove();
        }
    }

    /**
     * Replace placeholders in the message according to the receiver
     * @param receiver
     * @param message
     * @return updated message
     */
    public static String replacePlaceholders(CommandSender receiver, String message){
        if(message == null || message.isEmpty()) return "";

        for(PlaceholderInterface api : apis){
            message = api.replacePlaceholders(receiver, message);
        }

        return message;
    }

    /**
     * @return true if APIs are registered (including Internal), otherwise false
     */
    public static boolean hasHooks(){
        return apis != null ? true : false;
    }
}
