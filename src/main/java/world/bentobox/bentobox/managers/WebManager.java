package world.bentobox.bentobox.managers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonParseException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.TheBusyBiscuit.GitHubWebAPI4Java.GitHubWebAPI;
import io.github.TheBusyBiscuit.GitHubWebAPI4Java.objects.repositories.GitHubRepository;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.web.catalog.CatalogEntry;

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
                // Set connection interval to be at least 60 minutes.
                connectionInterval = Math.max(connectionInterval, 60 * 20 * 60L);
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> requestGitHubData(true), 20L, connectionInterval);
            }
        }
    }

    public void requestGitHubData(boolean clearCache) {
        getGitHub().ifPresent(gh -> {
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Downloading data from GitHub...");
            }
            GitHubRepository repo;
            try {
                repo = new GitHubRepository(gh, "BentoBoxWorld/weblink");
            } catch (Exception e) {
                plugin.logError("An unhandled exception occurred when trying to connect to GitHub...");
                plugin.logStacktrace(e);

                // Stop the execution of the method right away.
                return;
            }

            // Downloading the data
            String tagsContent = getContent(repo, "catalog/tags.json");
            String topicsContent = getContent(repo, "catalog/topics.json");
            String catalogContent = getContent(repo, "catalog/catalog.json");
            
            // People were concerned that the download took ages, so we need to tell them it's over now.
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Successfully downloaded data from GitHub.");
            }

            /* Parsing the data */

            // Register the tags translations in the locales
            if (!tagsContent.isEmpty()) {
                try {
                    JsonObject tags = new JsonParser().parse(tagsContent).getAsJsonObject();
                    tags.entrySet().forEach(entry -> plugin.getLocalesManager().getLanguages().values().forEach(locale -> {
                        JsonElement translation = entry.getValue().getAsJsonObject().get(locale.toLanguageTag());
                        if (translation != null) {
                            locale.set("catalog.tags." + entry.getKey(), translation.getAsString());
                        }
                    }));
                } catch (JsonParseException e) {
                    plugin.log("Could not update the Catalog Tags: the gathered JSON data is malformed.");
                }
            }

            // Register the topics translations in the locales
            if (!topicsContent.isEmpty()) {
                try {
                    JsonObject topics = new JsonParser().parse(topicsContent).getAsJsonObject();
                    topics.entrySet().forEach(entry -> plugin.getLocalesManager().getLanguages().values().forEach(locale -> {
                        JsonElement translation = entry.getValue().getAsJsonObject().get(locale.toLanguageTag());
                        if (translation != null) {
                            locale.set("catalog.topics." + entry.getKey(), translation.getAsString());
                        }
                    }));
                } catch (JsonParseException e) {
                    plugin.log("Could not update the Catalog Topics: the gathered JSON data is malformed.");
                }
            }

            // Register the catalog data
            if (!catalogContent.isEmpty()) {
                try {
                    JsonObject catalog = new JsonParser().parse(catalogContent).getAsJsonObject();

                    if (clearCache) {
                        this.addonsCatalog.clear();
                        this.gamemodesCatalog.clear();
                    }

                    catalog.getAsJsonArray("gamemodes").forEach(gamemode -> gamemodesCatalog.add(new CatalogEntry(gamemode.getAsJsonObject())));
                    catalog.getAsJsonArray("addons").forEach(addon -> addonsCatalog.add(new CatalogEntry(addon.getAsJsonObject())));
                } catch (JsonParseException e) {
                    plugin.log("Could not update the Catalog content: the gathered JSON data is malformed.");
                }
            }
        });
    }

    /**
     *
     * @param repo
     * @param fileName
     * @return
     * @since 1.8.0
     */
    @NonNull
    private String getContent(@NonNull GitHubRepository repo, String fileName) {
        try {
            String content = repo.getContent(fileName).getContent().replaceAll("\\n", "");
            return new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
        } catch (IllegalAccessException e) {
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Could not connect to GitHub.");
            }
        } catch (Exception e) {
            plugin.logError("An unhandled exception occurred when downloading '" + fileName + "' from GitHub...");
            plugin.logStacktrace(e);
        }
        return "";
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
