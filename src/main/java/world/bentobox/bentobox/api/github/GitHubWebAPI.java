package world.bentobox.bentobox.api.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import world.bentobox.bentobox.api.github.objects.repositories.GitHubRepository;

/**
 * Handles interactions with the GitHub API.
 * This is a rewrite of <a href="https://github.com/Poslovitch/GitHubWebAPI4">...</a>, which is now out of date
 * and this code only implements parts that are used by BentoBox and not all of it.
 */
public class GitHubWebAPI {

    private static final String API_BASE_URL = "https://api.github.com/";
    private static final long RATE_LIMIT_INTERVAL_MS = 1000; // 1 second
    private static long lastRequestTime = 0;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private synchronized String fetchRaw(String endpoint) throws IOException, URISyntaxException {
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

        URL url = new URI(API_BASE_URL + endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "BentoBox");

        int responseCode = connection.getResponseCode();
        if (responseCode == 403) {
            throw new IOException("GitHub API rate limit exceeded or access forbidden. Response code: " + responseCode);
        }

        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    /**
     * Fetches the content of a given API endpoint.
     *
     * @param endpoint The API endpoint to fetch.
     * @return The JSON response as a JsonObject.
     * @throws IOException If an error occurs during the request.
     * @throws URISyntaxException if URI syntax is wrong
     */
    public synchronized JsonObject fetch(String endpoint) throws IOException, URISyntaxException {
        return JsonParser.parseString(fetchRaw(endpoint)).getAsJsonObject();
    }

    /**
     * Fetches the content of a given API endpoint that returns a JSON array.
     *
     * @param endpoint The API endpoint to fetch.
     * @return The JSON response as a JsonArray.
     * @throws IOException If an error occurs during the request.
     * @throws URISyntaxException if URI syntax is wrong
     */
    public synchronized JsonArray fetchArray(String endpoint) throws IOException, URISyntaxException {
        return JsonParser.parseString(fetchRaw(endpoint)).getAsJsonArray();
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
                throw new UncheckedIOException("Failed to fetch data from GitHub API", e);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid GitHub API endpoint URI", e);
            }
        }, executor);
    }

    public GitHubRepository getRepository(String username, String repo) {
        return new GitHubRepository(this, username + "/" + repo);
    }
}
