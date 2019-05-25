package world.bentobox.bentobox.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.web.catalog.CatalogEntry;
import world.bentobox.githubapi4java.GitHub;
import world.bentobox.githubapi4java.objects.GitHubGist;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles web-related stuff.
 * Should be instantiated after all addons are loaded.
 * @author Poslovitch
 * @since 1.3.0
 */
public class WebManager {

    private @NonNull BentoBox plugin;
    private @Nullable GitHub gitHub;
    private @NonNull List<CatalogEntry> addonsCatalog;
    private @NonNull List<CatalogEntry> gamemodesCatalog;

    public WebManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.addonsCatalog = new ArrayList<>();
        this.gamemodesCatalog = new ArrayList<>();

        // Setup the GitHub connection
        if (plugin.getSettings().isGithubDownloadData()) {
            this.gitHub = new GitHub();

            long connectionInterval = plugin.getSettings().getGithubConnectionInterval() * 20L * 60L;
            if (connectionInterval <= 0) {
                // If below 0, it means we shouldn't run this as a repeating task.
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> requestGitHubData(true), 20L);
            } else {
                // Set connection interval to be at least 15 minutes.
                connectionInterval = Math.max(connectionInterval, 15 * 20 * 60L);
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> requestGitHubData(true), 20L, connectionInterval);
            }
        }
    }

    public void requestGitHubData(boolean clearCache) {
        getGitHub().ifPresent(gh -> {
            if (clearCache) {
                gh.clearCache(); // TODO might be better to not clear cache, check
                this.addonsCatalog.clear();
                this.gamemodesCatalog.clear();
            }

            plugin.log("Updating data from GitHub...");
            try {
                String catalogContent = new GitHubGist(gh, "bccabc20bce17f358d0f94bbbe83babd").getRawResponseAsJson()
                        .getAsJsonObject().getAsJsonObject("files").getAsJsonObject("catalog.json").get("content").getAsString()
                        .replace("\n", "").replace("\\", "");

                JsonObject catalog = new JsonParser().parse(catalogContent).getAsJsonObject();
                catalog.getAsJsonArray("gamemodes").forEach(gamemode -> gamemodesCatalog.add(new CatalogEntry(gamemode.getAsJsonObject())));
                catalog.getAsJsonArray("addons").forEach(addon -> addonsCatalog.add(new CatalogEntry(addon.getAsJsonObject())));
            } catch (Exception e) {
                plugin.logError("An error occurred when downloading or parsing data from GitHub...");
                plugin.logStacktrace(e);
            }
        });
    }

    /**
     * Returns the contents of the addons catalog (may be an empty list).
     * @return the contents of the addons catalog.
     * @since 1.5.0
     */
    @NonNull
    public List<CatalogEntry> getAddonsCatalog() {
        return addonsCatalog;
    }

    /**
     * Returns the contents of the gamemodes catalog (may be an empty list).
     * @return the contents of the gamemodes catalog.
     * @since 1.5.0
     */
    @NonNull
    public List<CatalogEntry> getGamemodesCatalog() {
        return gamemodesCatalog;
    }

    /**
     * Returns an optional that may contain the {@link GitHub} instance only and only if {@link Settings#isGithubDownloadData()} is {@code true}.
     * @return the GitHub instance.
     * @since 1.5.0
     */
    @NonNull
    public Optional<GitHub> getGitHub() {
        return Optional.ofNullable(gitHub);
    }
}
