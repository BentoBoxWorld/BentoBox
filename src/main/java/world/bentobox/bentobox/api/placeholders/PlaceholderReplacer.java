package world.bentobox.bentobox.api.placeholders;

import world.bentobox.bentobox.api.user.User;

@FunctionalInterface
public interface PlaceholderReplacer {

    String onReplace(User user);
}
