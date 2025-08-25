package world.bentobox.bentobox.api.github.objects.repositories;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import world.bentobox.bentobox.api.github.GitHubWebAPI;

/**
 * Represents a GitHub repository and provides methods to fetch its data.
 */
public class GitHubRepository {

    private final GitHubWebAPI api;
    private final String fullName;

    public GitHubRepository(GitHubWebAPI api, String fullName) {
        this.api = api;
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * Fetches the content of a file in the repository.
     * 
     * @param path The path to the file.
     * @return A GitHubFile object representing the file content.
     * @throws Exception If an error occurs during the request.
     */
    public GitHubFile getContent(String path) throws Exception {
        return new GitHubFile(api, this, "/contents/" + path);
    }

    /**
     * Fetches the list of contributors to the repository.
     * 
     * @return A list of GitHubContributor objects.
     * @throws Exception If an error occurs during the request.
     */
    public List<GitHubContributor> getContributors() throws Exception {
        JsonArray response = api.fetch("repos/" + fullName + "/contributors").getAsJsonArray();
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
}
