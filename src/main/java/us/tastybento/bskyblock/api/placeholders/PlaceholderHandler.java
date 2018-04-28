package us.tastybento.bskyblock.api.placeholders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;

/**
 * Handles hooks with other Placeholder APIs.
 *
 * @author Poslovitch, Tastybento
 */
public class PlaceholderHandler {
    private static final String PACKAGE = "us.tastybento.bskyblock.api.placeholders.hooks.";
    
    // This class should never be instantiated (all methods are static)
    private PlaceholderHandler() {}
    
    /**
     * List of API classes in the package specified above (except the Internal one)
     */
    private static final String[] HOOKS = {
            //TODO
    };

    private static List<PlaceholderAPIInterface> apis = new ArrayList<>();

    /**
     * Register placeholders and hooks
     * @param plugin - BSkyBlock plugin object
     */
    public static void register(BSkyBlock plugin){

        // Load Internal Placeholder API
        try{
            Class<?> clazz = Class.forName(PACKAGE + "InternalPlaceholderImpl");
            PlaceholderAPIInterface internal = (PlaceholderAPIInterface)clazz.newInstance();
            apis.add(internal);
        } catch (Exception e){
            // Should never happen.
            plugin.logError("Failed to load default placeholder API");
        }

        // Load hooks
        for(String hook : HOOKS){
            if(plugin.getServer().getPluginManager().isPluginEnabled(hook)){
                try{
                    Class<?> clazz = Class.forName(PACKAGE + hook + "PlaceholderImpl");
                    PlaceholderAPIInterface api = (PlaceholderAPIInterface)clazz.newInstance();
                    if(api.register(plugin)){
                        plugin.log("Hooked placeholders into " + hook); // since Java 8, we can use Supplier , which will be evaluated lazily
                        apis.add(api);
                    } else {
                        plugin.log("Failed to hook placeholders into " + hook);
                    }
                } catch (Exception e){
                    plugin.log("Failed to hook placeholders into " + hook);
                }
            }
        }
    }

    /**
     * Unregister placeholder hooks
     * @param plugin - BSkyBlock plugin object
     */
    public static void unregister(BSkyBlock plugin){
        Iterator<PlaceholderAPIInterface> it = apis.iterator();
        while (it.hasNext()) {
            PlaceholderAPIInterface api = it.next();
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
    public static String replacePlaceholders(User receiver, String message){
        for(PlaceholderAPIInterface api : apis){
            message = api.replacePlaceholders(receiver, message);
        }

        return message;
    }

    /**
     * @return true if APIs are registered (including Internal), otherwise false
     */
    public static boolean hasHooks(){
        return apis != null;
    }
}
