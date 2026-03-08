package world.bentobox.bentobox.api.github.objects.repositories;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import world.bentobox.bentobox.api.github.GitHubWebAPI;

/**
 * Represents a GitHub repository and provides methods to fetch its data.
 */
public record GitHubRepository(GitHubWebAPI api, String fullName) {

    /**
     * Fetches the content of a file in the repository.
     *
     * @param path The path to the file.
     * @return A GitHubFile object representing the file content.
     */
    public GitHubFile getContent(String path) {
        return new GitHubFile(api, this, "/contents/" + path);
    }

    /**
     * Fetches the list of contributors to the repository.
     *
     * @return A list of GitHubContributor objects.
     * @throws Exception If an error occurs during the request.
     */
    public List<GitHubContributor> getContributors() throws Exception {
        JsonArray response = api.fetchArray("repos/" + fullName + "/contributors");
        List<GitHubContributor> contributors = new ArrayList<>();
        response.forEach(element -> {
            JsonObject contributor = element.getAsJsonObject();
            contributors.add(new GitHubContributor(
                    contributor.get("login").getAsString(),
                    contributor.get("contributions").getAsInt()
            ));
        });
        return contributors;
    }

    /**
     * Fetches the name of the latest tag for this repository.
     *
     * @return the latest tag name (e.g. "3.11.2"), or empty string if none.
     * @throws Exception if an error occurs during the request.
     */
    public String getLatestTagName() throws Exception {
        JsonArray tags = api.fetchArray("repos/" + fullName + "/tags");
        if (tags.isEmpty()) return "";
        return tags.get(0).getAsJsonObject().get("name").getAsString();
    }
}
