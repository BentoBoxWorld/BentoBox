package world.bentobox.bentobox.api.github.objects.repositories;

import java.io.IOException;
import java.net.URISyntaxException;

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
     * @return the content of this file in Base64 or nothing if the connection to the GitHub API could not be established.
     * @throws IOException - If an error occurs during the request.
     */
    public String getContent() throws IllegalAccessException {
        JsonObject response;
        try {
            response = api.fetch(path);
        } catch (IOException | URISyntaxException e) {
            // Cannot get a connection for some reason
            return "";
        }
        return response.get("content").getAsString();
    }
}
