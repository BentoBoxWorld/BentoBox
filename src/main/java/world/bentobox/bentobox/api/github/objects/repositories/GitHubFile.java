package world.bentobox.bentobox.api.github.objects.repositories;

import java.io.IOException;

import com.google.gson.JsonObject;

import world.bentobox.bentobox.api.github.GitHubWebAPI;

/**
 * Represents a file or a directory.
 */
public class GitHubFile {

    private final GitHubWebAPI api;
    private final String path;

    public GitHubFile(GitHubWebAPI api, GitHubRepository gitHubRepository, String fullpath) {
        this.api = api;
        this.path = "repos/" + gitHubRepository.getFullName() + fullpath;
    }

    /**
     * Returns the content of this file.
     * @return the content of this file in Base64
     * @throws IllegalAccessException if the connection to the GitHub API could not be established.
     * @throws IOException - If an error occurs during the request.
     */
    public String getContent() throws IllegalAccessException, IOException {
        JsonObject response = api.fetch(path);
        return response.get("content").getAsString();
    }
}
