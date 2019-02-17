package world.bentobox.bentobox.managers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.github.GitHubConnector;

/**
 * Handles web-related stuff.
 * Should be instantiated after all addons are loaded.
 * @author Poslovitch
 * @since 1.3.0
 */
public class WebManager {

    private @NonNull BentoBox plugin;
    private @NonNull GitHubConnector bentoBoxGitHubConnector;
    private @NonNull Map<@NonNull Addon, @NonNull GitHubConnector> gitHubConnectors = new LinkedHashMap<>();

    public WebManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.bentoBoxGitHubConnector = new GitHubConnector("BentoBoxWorld/BentoBox");
        setupAddonsGitHubConnectors();
    }

    /**
     * Setups the {@code GitHubConnector} instances for each addons.
     */
    private void setupAddonsGitHubConnectors() {
        plugin.getAddonsManager().getEnabledAddons().stream()
                .filter(addon -> !addon.getDescription().getRepository().isEmpty())
                .forEach(addon -> gitHubConnectors.put(addon, new GitHubConnector(addon.getDescription().getRepository())));
    }

    /**
     * Connects all the {@link GitHubConnector} to GitHub to retrieve data.
     */
    public void requestGitHubData() {
        Bukkit.getScheduler().runTask(plugin, () -> bentoBoxGitHubConnector.connect());
        gitHubConnectors.values().forEach(gitHubConnector -> Bukkit.getScheduler().runTask(plugin, gitHubConnector::connect));
    }
}
