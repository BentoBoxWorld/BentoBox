package world.bentobox.bentobox.api.placeholders.placeholderapi;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;

public class BentoBoxPlaceholderExpansion extends BasicPlaceholderExpansion {
    private final BentoBox plugin;

    public BentoBoxPlaceholderExpansion(BentoBox plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public @NonNull String getName() {
        return plugin.getName();
    }

    @Override
    public @NonNull String getAuthor() {
        return "Tastybento and Poslovitch";
    }

    @Override
    public @NonNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
}