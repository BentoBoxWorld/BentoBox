package us.tastybento.bskyblock.api.placeholders.hooks;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.placeholders.Placeholder;
import us.tastybento.bskyblock.api.placeholders.PlaceholderAPIInterface;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.lists.Placeholders;

/**
 * Built-in placeholder API
 *
 * @author Poslovitch
 */
public class InternalPlaceholderImpl implements PlaceholderAPIInterface {

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
    public String replacePlaceholders(User receiver, String message) {
        if(message == null || message.isEmpty()) {
            return "";
        }

        for(Placeholder placeholder : Placeholders.values()){
            String identifier = "%" + placeholder.getIdentifier() + "%";
            message = message.replaceAll(identifier, placeholder.getRequest().request(receiver));
        }

        return message;
    }

}
