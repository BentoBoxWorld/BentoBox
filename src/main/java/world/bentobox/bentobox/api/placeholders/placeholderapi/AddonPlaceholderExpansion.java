package world.bentobox.bentobox.api.placeholders.placeholderapi;

import world.bentobox.bentobox.api.addons.Addon;

public class AddonPlaceholderExpansion extends BasicPlaceholderExpansion {
    private final Addon addon;

    public AddonPlaceholderExpansion(Addon addon) {
        this.addon = addon;
    }

    @Override
    public String getName() {
        return addon.getDescription().getName();
    }

    @Override
    public String getAuthor() {
        return addon.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return addon.getDescription().getVersion();
    }
}
