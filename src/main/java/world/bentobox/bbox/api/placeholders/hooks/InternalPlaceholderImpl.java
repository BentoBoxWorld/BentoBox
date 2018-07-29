package world.bentobox.bbox.api.placeholders.hooks;

import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.api.placeholders.Placeholder;
import world.bentobox.bbox.api.placeholders.PlaceholderAPIInterface;
import world.bentobox.bbox.api.user.User;
import world.bentobox.bbox.lists.Placeholders;

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
    public boolean register(BentoBox plugin) {
        return true;
    }

    @Override
    public void unregister(BentoBox plugin) {
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
