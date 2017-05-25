package us.tastybento.bskyblock.util.placeholders.hooks;

import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.placeholders.PlaceholderInterface;
import us.tastybento.bskyblock.util.placeholders.Placeholders;
import us.tastybento.bskyblock.util.placeholders.Placeholders.Placeholder;

/**
 * Built-in placeholder API
 * 
 * @author Poslovitch
 */
public class InternalPlaceholderImpl implements PlaceholderInterface{

    @Override
    public String getName() {
        return "Internal";
    }

    @Override
    public boolean register(BSkyBlock plugin) {
        return true;
    }

    @Override
    public void unregister(BSkyBlock plugin) {
        // Useless : it would disable the placeholders.
    }

    @Override
    public String replacePlaceholders(CommandSender receiver, String message) {
        if(message == null || message.isEmpty()) return "";

        for(Placeholder placeholder : Placeholders.getPlaceholders()){
            String identifier = "{" + placeholder.getIdentifier() + "}";
            message = message.replaceAll(Pattern.quote(identifier), placeholder.onRequest(receiver));
        }

        return message;
    }

}
