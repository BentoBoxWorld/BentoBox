package world.bentobox.bentobox.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Fetches UUID for a player name from the Internet
 * @since 1.24.1
 */
public class UUIDFetcher {
    private static final String API_URL = "https://playerdb.co/api/player/minecraft/%s";

    @Nullable
    public static UUID getUUID(@NotNull String name) {
        name = name.toLowerCase(); // Had some issues with upper-case letters in the username, so I added this to make sure that doesn't happen.

        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(String.format(API_URL, name)).toURL()
                    .openConnection();

            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            connection.setReadTimeout(5000);

            // These connection parameters need to be set or the API won't accept the connection.

            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null)
                    response.append(line);

                final JsonElement parsed = JsonParser.parseString(response.toString());

                if (parsed == null || !parsed.isJsonObject()) {
                    return null;
                }

                JsonObject data = parsed.getAsJsonObject(); // Read the returned JSON data.

                return UUID.fromString(data.get("data").getAsJsonObject().get("player").getAsJsonObject().get("id") // Grab the UUID.
                        .getAsString());
            }
        } catch (Exception ignored) {
            // Ignoring exception since this is usually caused by non-existent usernames.
        }

        return null;
    }
}