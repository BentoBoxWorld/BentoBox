package world.bentobox.bentobox.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.TheBusyBiscuit.GitHubWebAPI4Java.GitHubWebAPI;
import io.github.TheBusyBiscuit.GitHubWebAPI4Java.objects.repositories.GitHubRepository;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.web.catalog.CatalogEntry;

import java.util.ArrayList;
import java.util.Base64;
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
    private @Nullable GitHubWebAPI gitHub;
    private @NonNull List<CatalogEntry> addonsCatalog;
    private @NonNull List<CatalogEntry> gamemodesCatalog;

    public WebManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.addonsCatalog = new ArrayList<>();
        this.gamemodesCatalog = new ArrayList<>();

        // Setup the GitHub connection
        if (plugin.getSettings().isGithubDownloadData()) {
            this.gitHub = new GitHubWebAPI();

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
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Downloading data from GitHub...");
            }
            GitHubRepository repo = new GitHubRepository(gh, "BentoBoxWorld/weblink");
            String catalogContent = "";
            // Downloading the data
            try {
                catalogContent = repo.getContent("catalog/catalog.json").getContent().replaceAll("\\n", "");
                catalogContent = new String(Base64.getDecoder().decode(catalogContent));
            } catch (Exception e) {
                plugin.logError("An error occurred when downloading from GitHub...");
                plugin.logStacktrace(e);
            }

            // Parsing the data
            if (!catalogContent.isEmpty()) {
                if (clearCache) {
                    gh.clearCache();
                    this.addonsCatalog.clear();
                    this.gamemodesCatalog.clear();
                }

                JsonObject catalog = new JsonParser().parse(catalogContent).getAsJsonObject();
                catalog.getAsJsonArray("gamemodes").forEach(gamemode -> gamemodesCatalog.add(new CatalogEntry(gamemode.getAsJsonObject())));
                catalog.getAsJsonArray("addons").forEach(addon -> addonsCatalog.add(new CatalogEntry(addon.getAsJsonObject())));
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
     * Returns an optional that may contain the {@link GitHubWebAPI} instance only and only if {@link Settings#isGithubDownloadData()} is {@code true}.
     * @return the GitHub instance.
     * @since 1.5.0
     */
    @NonNull
    public Optional<GitHubWebAPI> getGitHub() {
        return Optional.ofNullable(gitHub);
    }
}
