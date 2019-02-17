package world.bentobox.bentobox.api.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Somehow wraps {@link URL} and {@link java.net.URLConnection} to avoid boilerplate code when accessing to the GitHub API.
 * @author Poslovitch
 * @since 1.3.0
 */
public class GitHubURL {

    private final @NonNull URL url;

    public GitHubURL(@NonNull String repository, @Nullable String suffix) throws MalformedURLException {
        suffix = (suffix != null && !suffix.isEmpty()) ? "/" + suffix : "";
        this.url = new URL("https://api.github.com/repos/" + repository + suffix);
    }

    @NonNull
    public URL toURL() {
        return url;
    }

    public HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(2500);
        connection.addRequestProperty("User-Agent", "BentoBox GitHubLink (@BentoBoxWorld)");
        connection.setDoOutput(true);
        return connection;
    }
}
