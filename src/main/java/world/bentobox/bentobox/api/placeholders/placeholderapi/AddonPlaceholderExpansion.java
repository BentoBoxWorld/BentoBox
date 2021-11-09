package world.bentobox.bentobox.api.placeholders.placeholderapi;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.addons.Addon;

public class AddonPlaceholderExpansion extends BasicPlaceholderExpansion {
    private final Addon addon;

    public AddonPlaceholderExpansion(Addon addon) {
        this.addon = addon;
    }

    @Override
    public @NonNull String getName() {
        return addon.getDescription().getName();
    }

    @Override
    public @NonNull String getAuthor() {
        return addon.getDescription().getAuthors().get(0);
    }

    @Override
    public @NonNull String getVersion() {
        return addon.getDescription().getVersion();
    }
}
