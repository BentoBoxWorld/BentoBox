package world.bentobox.bentobox.api.placeholders.hooks;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.placeholders.Placeholder;
import world.bentobox.bentobox.api.placeholders.PlaceholderAPIInterface;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Placeholders;

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
