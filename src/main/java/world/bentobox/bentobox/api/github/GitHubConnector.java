package world.bentobox.bentobox.api.github;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * Handles connection to the GitHub API, retrieves data and handles the {@link Repository} data that emerges from it.
 * @author Poslovitch
 * @since 1.3.0
 */
public class GitHubConnector {

    private @NonNull String repositoryName;
    private Repository repository;

    public GitHubConnector(@NonNull String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void connect() {
        JsonElement repositoryData;
        JsonElement releasesData;
        JsonElement contributorData;

        // Get the data
        try {
            repositoryData = getData(null);
            // TODO getting other data is pointless if we couldn't get the data from the repository
            contributorData = getData("contributors");
            releasesData = getData("releases");
        } catch (IOException e) {
            BentoBox.getInstance().logStacktrace(e);
            // TODO do not override data instead
            return;
        }

        // Parse the data
        /* It must be done in a specific order:
            1. repository
            2. contributors
            3. releases
         */
        parseRepositoryData(repositoryData);
        parseContributorsData(contributorData);
        parseReleasesData(releasesData);
    }

    @NonNull
    private JsonElement getData(@Nullable String suffix) throws IOException {
        HttpURLConnection connection = new GitHubURL(getRepositoryName(), suffix).openConnection();
        String data = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n"));
        return new JsonParser().parse(data);
    }

    private void parseRepositoryData(@NonNull JsonElement jsonElement) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        Repository.Builder builder = new Repository.Builder(repositoryName.split("/")[0], repositoryName.split("/")[1])
                .stars(jsonObject.get("stargazers_count").getAsInt())
                .forks(jsonObject.get("forks_count").getAsInt())
                .openIssues(jsonObject.get("open_issues_count").getAsInt())
                .latestCommit(Util.parseGitHubDate(jsonObject.get("pushed_at").getAsString()));

        this.repository = builder.build();
    }

    private void parseContributorsData(@NonNull JsonElement jsonElement) {
        for (JsonElement contributorElement : jsonElement.getAsJsonArray()) {
            JsonObject contributor = contributorElement.getAsJsonObject();
            this.repository.getContributors().add(new Contributor(contributor.get("login").getAsString(), contributor.get("contributions").getAsInt()));
        }
    }

    private void parseReleasesData(@NonNull JsonElement jsonElement) {
        for (JsonElement releaseElement : jsonElement.getAsJsonArray()) {
            JsonObject release = releaseElement.getAsJsonObject();

            String tag = release.get("tag_name").getAsString();
            String url = repository.getUrl() + "/releases/tag/" + tag;

            Release.Builder builder = new Release.Builder(release.get("name").getAsString(), tag, url)
                    .preRelease(release.get("prerelease").getAsBoolean())
                    .publishedAt(Util.parseGitHubDate( release.get("published_at").getAsString()));

            // Run through the releases assets and try to find the correct one
            for (JsonElement assetElement : release.get("assets").getAsJsonArray()) {
                JsonObject asset = assetElement.getAsJsonObject();

                String assetName = asset.get("name").getAsString();
                if (assetName.endsWith(".jar") && !assetName.contains("javadoc") && !assetName.contains("sources")) {
                    // We found our asset!

                    builder.downloadUrl(asset.get("browser_download_url").getAsString())
                            .downloadSize(asset.get("size").getAsLong())
                            .downloadCount(asset.get("download_count").getAsInt());

                    break;
                }
            }

            this.repository.getReleases().add(builder.build());
        }
    }

    @NonNull
    public String getRepositoryName() {
        return repositoryName;
    }

    public Repository getRepository() {
        return repository;
    }
}
