package world.bentobox.bentobox.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import org.bukkit.Bukkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.github.GitHubWebAPI;
import world.bentobox.bentobox.api.github.objects.repositories.GitHubContributor;
import world.bentobox.bentobox.api.github.objects.repositories.GitHubRepository;
import world.bentobox.bentobox.web.catalog.CatalogEntry;
import world.bentobox.bentobox.web.credits.Contributor;

/**
 * Handles web-related stuff.
 * Should be instantiated after all addons are loaded.
 * 
 * @author Poslovitch
 * @since 1.3.0
 */
public class WebManager {

    @NonNull
    private final BentoBox plugin;
    @Nullable
    private GitHubWebAPI gitHub;
    @NonNull
    private final List<CatalogEntry> addonsCatalog;
    @NonNull
    private final List<CatalogEntry> gamemodesCatalog;
    @NonNull
    private final Map<String, List<Contributor>> contributors;

    public WebManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        this.addonsCatalog = new ArrayList<>();
        this.gamemodesCatalog = new ArrayList<>();
        this.contributors = new HashMap<>();

        // Set up the GitHub connection
        if (plugin.getSettings().isGithubDownloadData()) {
            this.gitHub = new GitHubWebAPI();

            long connectionInterval = plugin.getSettings().getGithubConnectionInterval() * 20L * 60L;
            if (connectionInterval <= 0) {
                // If below 0, it means we shouldn't run this as a repeating task.
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::requestGitHubData, 20L);
            } else {
                // Set connection interval to be at least 60 minutes.
                connectionInterval = Math.max(connectionInterval, 60 * 20 * 60L);
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::requestGitHubData, 20L, connectionInterval);
            }
        }
    }

    public void requestGitHubData() {
        getGitHub().ifPresent(gh -> {
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Downloading data from GitHub...");
                plugin.log("Updating the Catalog...");
            }
            GitHubRepository weblinkRepo;
            try {
                weblinkRepo = new GitHubRepository(gh, "BentoBoxWorld/weblink");
            } catch (Exception e) {
                if (plugin.getSettings().isLogGithubDownloadData()) {
                    plugin.logError("An unhandled exception occurred when connecting to the GitHub weblink..");
                    plugin.logStacktrace(e);
                }
                weblinkRepo = null;
            }

            if (weblinkRepo != null) {
                // Downloading the data
                String tagsContent = getContent(weblinkRepo, "catalog/tags.json");
                String topicsContent = getContent(weblinkRepo, "catalog/topics.json");
                String catalogContent = getContent(weblinkRepo, "catalog/catalog.json");

                /* Parsing the data */
                parseCatalogContent(tagsContent, topicsContent, catalogContent);
            }

            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Updating Contributors information...");
            }

            List<String> repositories = new ArrayList<>();
            // Gather all the repositories of installed addons.
            repositories.add("BentoBoxWorld/BentoBox");
            repositories.addAll(plugin.getAddonsManager().getEnabledAddons().stream()
                    .map(addon -> addon.getDescription().getRepository()).filter(repo -> !repo.isEmpty()).toList());

            /* Download the contributors */
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Gathering contribution data for: " + String.join(", ", repositories));
            }

            for (String repository : repositories) {
                GitHubRepository repo;
                try {
                    repo = new GitHubRepository(gh, repository);
                } catch (Exception e) {
                    if (plugin.getSettings().isLogGithubDownloadData()) {
                        plugin.logError("An unhandled exception occurred when gathering contributors data from the '"
                                + repository + "' repository...");
                        plugin.logStacktrace(e);
                    }
                    repo = null;
                }
                if (repo != null) {
                    gatherContributors(repo);
                }
            }

            // People were concerned that the download took ages, so we need to tell them
            // it's over now.
            if (plugin.getSettings().isLogGithubDownloadData()) {
                plugin.log("Successfully downloaded data from GitHub.");
            }

            if (plugin.getSettings().isCheckBentoBoxUpdates()) {
                checkForUpdates(gh);
            }
        });
    }

    private void parseCatalogContent(String tagsContent, String topicsContent, String catalogContent) {
        // Register the tags translations in the locales
        if (!tagsContent.isEmpty()) {
            try {
                JsonObject tags = JsonParser.parseString(tagsContent).getAsJsonObject();
                tags.entrySet().forEach(entry -> plugin.getLocalesManager().getLanguages().values().forEach(locale -> {
                    JsonElement translation = entry.getValue().getAsJsonObject().get(locale.toLanguageTag());
                    if (translation != null) {
                        locale.set("catalog.tags." + entry.getKey(), translation.getAsString());
                    }
                }));
            } catch (JsonParseException e) {
                if (plugin.getSettings().isLogGithubDownloadData()) {
                    plugin.log("Could not update the Catalog Tags: the gathered JSON data is malformed.");
                }
            }
        }

        // Register the topics translations in the locales
        if (!topicsContent.isEmpty()) {
            try {
                JsonObject topics = JsonParser.parseString(topicsContent).getAsJsonObject();
                topics.entrySet().forEach(entry -> plugin.getLocalesManager().getLanguages().values().forEach(locale -> {
                    JsonElement translation = entry.getValue().getAsJsonObject().get(locale.toLanguageTag());
                    if (translation != null) {
                        locale.set("catalog.topics." + entry.getKey(), translation.getAsString());
                    }
                }));
            } catch (JsonParseException e) {
                if (plugin.getSettings().isLogGithubDownloadData()) {
                    plugin.log("Could not update the Catalog Topics: the gathered JSON data is malformed.");
                }
            }
        }

        // Register the catalog data
        if (!catalogContent.isEmpty()) {
            try {
                JsonObject catalog = JsonParser.parseString(catalogContent).getAsJsonObject();

                this.addonsCatalog.clear();
                this.gamemodesCatalog.clear();

                catalog.getAsJsonArray("gamemodes")
                        .forEach(gamemode -> gamemodesCatalog.add(new CatalogEntry(gamemode.getAsJsonObject())));
                catalog.getAsJsonArray("addons")
                        .forEach(addon -> addonsCatalog.add(new CatalogEntry(addon.getAsJsonObject())));
            } catch (JsonParseException e) {
                if (plugin.getSettings().isLogGithubDownloadData()) {
                    plugin.log("Could not update the Catalog content: the gathered JSON data is malformed.");
                }
            }
        }
    }

    @NonNull
    private String getContent(@NonNull GitHubRepository repo, String fileName) {
        try {
            String content = repo.getContent(fileName).getContent();
            return new String(DatatypeConverter.parseBase64Binary(content.replace("_", "/")));
        } catch (Exception e) {
            // Silently fail
        }
        return "";
    }

    private void checkForUpdates(@NonNull GitHubWebAPI gh) {
        try {
            String currentVersion = plugin.getPluginMeta().getVersion();
            if (currentVersion.contains("LOCAL")) return;
            String latestTag = new GitHubRepository(gh, "BentoBoxWorld/BentoBox").getLatestTagName();
            if (!latestTag.isEmpty() && isNewerVersion(currentVersion, latestTag)) {
                printUpdateBanner(currentVersion, latestTag);
            }
        } catch (Exception e) {
            // Silently fail — same pattern as gatherContributors()
        }
    }

    private void printUpdateBanner(String currentVersion, String latestTag) {
        var cs = Bukkit.getConsoleSender();
        cs.sendMessage("§6╔══════════════════════════════════════════════╗");
        cs.sendMessage("§6║  §e★  BentoBox Update Available!  ★           §6║");
        cs.sendMessage("§6║                                              §6║");
        cs.sendMessage("§6║  §7Current: §c" + currentVersion);
        cs.sendMessage("§6║  §7Latest:  §a" + latestTag);
        cs.sendMessage("§6║                                              §6║");
        cs.sendMessage("§6║  §7Get it: §bhttps://github.com/BentoBoxWorld/BentoBox/releases");
        cs.sendMessage("§6╚══════════════════════════════════════════════╝");
    }

    /**
     * Returns true if latestTag is strictly newer than currentVersion.
     * Only the numeric prefix before the first '-' is compared.
     */
    static boolean isNewerVersion(String currentVersion, String latestTag) {
        String current = currentVersion.split("-")[0];
        String latest = latestTag.replaceFirst("^v", "").split("-")[0];
        String[] cp = current.split("\\.");
        String[] lp = latest.split("\\.");
        int len = Math.max(cp.length, lp.length);
        for (int i = 0; i < len; i++) {
            int c = i < cp.length ? parseIntSafe(cp[i]) : 0;
            int l = i < lp.length ? parseIntSafe(lp[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private void gatherContributors(@NonNull GitHubRepository repo) {
        try {
            List<Contributor> addonContributors = new LinkedList<>();
            for (GitHubContributor gitHubContributor : repo.getContributors()) {
                addonContributors.add(
                        new Contributor(gitHubContributor.username(), gitHubContributor.contributionsAmount()));
            }
            contributors.put(repo.fullName(), addonContributors);
        } catch (Exception e) {
            // Silently fail
        }
    }

    /**
     * Returns the contents of the addons catalog (maybe an empty list).
     * 
     * @return the contents of the addons catalog.
     * @since 1.5.0
     */
    @NonNull
    public List<CatalogEntry> getAddonsCatalog() {
        return addonsCatalog;
    }

    /**
     * Returns the contents of the gamemodes catalog (maybe an empty list).
     * 
     * @return the contents of the gamemodes catalog.
     * @since 1.5.0
     */
    @NonNull
    public List<CatalogEntry> getGamemodesCatalog() {
        return gamemodesCatalog;
    }

    /**
     *
     * @param repository - name of the repo
     * @return list of contributors
     * @since 1.9.0
     */
    @NonNull
    public List<Contributor> getContributors(String repository) {
        return contributors.getOrDefault(repository, new ArrayList<>());
    }

    /**
     * Returns an optional that may contain the {@link GitHubWebAPI} instance only
     * and only if {@link Settings#isGithubDownloadData()} is {@code true}.
     * 
     * @return the GitHub instance.
     * @since 1.5.0
     */
    @NonNull
    public Optional<GitHubWebAPI> getGitHub() {
        return Optional.ofNullable(gitHub);
    }
}
