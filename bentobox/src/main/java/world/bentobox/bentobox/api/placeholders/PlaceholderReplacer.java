package world.bentobox.bentobox.api.placeholders;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.user.User;

@FunctionalInterface
public interface PlaceholderReplacer {

    @NonNull
    String onReplace(@Nullable User user);
}
