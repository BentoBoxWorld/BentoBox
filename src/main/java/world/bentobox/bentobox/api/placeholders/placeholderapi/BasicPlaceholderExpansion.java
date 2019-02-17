package world.bentobox.bentobox.api.placeholders.placeholderapi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;

abstract class BasicPlaceholderExpansion extends PlaceholderExpansion {
    private Map<String, PlaceholderReplacer> placeholders;

    BasicPlaceholderExpansion() {
        this.placeholders = new HashMap<>();
    }

    @Override
    public String getIdentifier() {
        return getName().toLowerCase(Locale.ENGLISH);
    }

    public void registerPlaceholder(String placeholder, PlaceholderReplacer replacer) {
        placeholders.putIfAbsent(placeholder, replacer);
    }

    @Override
    public String onPlaceholderRequest(Player p, String placeholder) {
        User user = User.getInstance(p);

        if (placeholders.containsKey(placeholder)) {
            return placeholders.get(placeholder).onReplace(user);
        }
        return null;
    }
}