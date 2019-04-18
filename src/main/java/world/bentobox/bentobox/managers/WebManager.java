package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;

/**
 * Handles web-related stuff.
 * Should be instantiated after all addons are loaded.
 * @author Poslovitch
 * @since 1.3.0
 */
public class WebManager {

    private @NonNull BentoBox plugin;

    public WebManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        if (plugin.getSettings().isGithubDownloadData()) {
            setupAddonsGitHubConnectors();
        }
    }

    /**
     * Setups the {@code GitHubConnector} instances for each addons.
     */
    private void setupAddonsGitHubConnectors() {
        plugin.getAddonsManager().getEnabledAddons().stream()
                .filter(addon -> !addon.getDescription().getRepository().isEmpty());
    }

    /**
     */
    public void requestGitHubData() {
        if (plugin.getSettings().isGithubDownloadData()) {
        }
    }
}
