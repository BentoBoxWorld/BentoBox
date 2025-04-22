package world.bentobox.bentobox.api.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import world.bentobox.bentobox.api.github.objects.repositories.GitHubRepository;

/**
 * Handles interactions with the GitHub API.
 * This is a rewrite of {@link https://github.com/Poslovitch/GitHubWebAPI4}, which is now out of date
 * and this code only implements parts that are used by BentoBox and not all of it.
 */
public class GitHubWebAPI {

    private static final String API_BASE_URL = "https://api.github.com/";
    private static final long RATE_LIMIT_INTERVAL_MS = 1000; // 1 second
    private static long lastRequestTime = 0;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Fetches the content of a given API endpoint.
     * 
     * @param endpoint The API endpoint to fetch.
     * @return The JSON response as a JsonObject.
     * @throws IOException If an error occurs during the request.
     */
    public synchronized JsonObject fetch(String endpoint) throws IOException {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;

        if (timeSinceLastRequest < RATE_LIMIT_INTERVAL_MS) {
            try {
                Thread.sleep(RATE_LIMIT_INTERVAL_MS - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while waiting for rate limit", e);
            }
        }

        lastRequestTime = System.currentTimeMillis();

        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "BentoBox"); // Add User-Agent header

        int responseCode = connection.getResponseCode();
        if (responseCode == 403) {
            throw new IOException("GitHub API rate limit exceeded or access forbidden. Response code: " + responseCode);
        }

        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            String response = scanner.useDelimiter("\\A").next();
            return JsonParser.parseString(response).getAsJsonObject();
        }
    }

    /**
     * Fetches the content of a given API endpoint asynchronously.
     * 
     * @param endpoint The API endpoint to fetch.
     * @return A CompletableFuture containing the JSON response as a JsonObject.
     */
    public CompletableFuture<JsonObject> fetchAsync(String endpoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetch(endpoint);
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch data from GitHub API", e);
            }
        }, executor);
    }

    public GitHubRepository getRepository(String username, String repo) {
        return new GitHubRepository(this, username + "/" + repo);
    }
}
