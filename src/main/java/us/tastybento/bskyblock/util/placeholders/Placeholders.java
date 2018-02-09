package us.tastybento.bskyblock.util.placeholders;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Register placeholders
 *
 * @author Poslovitch
 */
public class Placeholders {
    private static Set<Placeholder> placeholders = new HashSet<>();

    private BSkyBlock plugin;

    protected Placeholders(BSkyBlock plugin){
        this.plugin = plugin;
        register();
    }

    private void register(){
        /*      PLUGIN      */
        new Placeholder("bsb_name"){
            @Override
            public String onRequest(CommandSender receiver) {
                return plugin.getDescription().getName();
            }
        };

        new Placeholder("bsb_version") {

            @Override
            public String onRequest(CommandSender receiver) {
                return plugin.getDescription().getVersion();
            }
        };

        //TODO: add more placeholders
    }

    public static Set<Placeholder> getPlaceholders(){
        return placeholders;
    }

    public abstract class Placeholder{
        private String identifier;

        protected Placeholder(String identifier){
            this.identifier = identifier;
            placeholders.add(this);
        }

        public String getIdentifier(){
            return identifier;
        }

        public abstract String onRequest(CommandSender receiver);
    }
}
