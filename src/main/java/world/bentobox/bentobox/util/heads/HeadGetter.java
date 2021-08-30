package world.bentobox.bentobox.util.heads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.util.Pair;


/**
 * This class manages getting player heads for requester.
 * @author tastybento, BONNe1704
 */
public class HeadGetter {
    /**
     * Local cache for storing player heads.
     */
    private static final Map<String, HeadCache> cachedHeads = new HashMap<>();

    /**
     * Local cache for storing requested names and items which must be updated.
     */
    private static final Queue<Pair<String, PanelItem>> names = new LinkedBlockingQueue<>();

    /**
     * Requesters of player heads.
     */
    private static final Map<String, Set<HeadRequester>> headRequesters = new HashMap<>();

    /**
     * Instance of plugin.
     */
    private final BentoBox plugin;


    /**
     * @param plugin - plugin
     */
    public HeadGetter(BentoBox plugin) {
        this.plugin = plugin;
        this.runPlayerHeadGetter();
    }

    /**
     * @param panelItem - head to update
     * @param requester - callback class
     * @since 1.14.1
     */
    public static void getHead(PanelItem panelItem, HeadRequester requester) {
        // Freshen cache
        // If memory is an issue we sacrifice performance?
        // cachedHeads.values().removeIf(cache -> System.currentTimeMillis() - cache.getTimestamp() > TOO_LONG);

        HeadCache cache = cachedHeads.get(panelItem.getPlayerHeadName());

        // Get value from config. Multiply value to 60 000 as internally it uses milliseconds.
        // Config value stores minutes.
        long cacheTimeout = BentoBox.getInstance().getSettings().getPlayerHeadCacheTime() * 60 * 1000;

        // to avoid every time clearing stored heads (as they may become very large)
        // just check if requested cache exists and compare it with value from plugin settings.
        // If timestamp is set to 0, then it must be kept forever.
        // If settings time is set to 0, then always use cache.
        if (cache != null &&
            (cache.getTimestamp() == 0 || cacheTimeout == 0 || 
                 System.currentTimeMillis() - cache.getTimestamp() <= cacheTimeout))
        {
            panelItem.setHead(cachedHeads.get(panelItem.getPlayerHeadName()).getPlayerHead());
            requester.setHead(panelItem);
        }
        else
        {
            // Get the name
            headRequesters.computeIfAbsent(panelItem.getPlayerHeadName(), k -> new HashSet<>()).
                add(requester);
            names.add(new Pair<>(panelItem.getPlayerHeadName(), panelItem));
        }
    }


    /**
     * This method allows to add HeadCache object into local cache.
     * It will provide addons to use HeadGetter cache directly.
     * @param cache Cache object that need to be added into local cache.
     * @since 1.14.1
     */
    public static void addToCache(HeadCache cache)
    {
        cachedHeads.put(cache.getUserName(), cache);
    }


// ---------------------------------------------------------------------
// Section: Private methods
// ---------------------------------------------------------------------


    /**
     * This is main task that runs once every Settings#ticksBetweenCalls ticks and tries to get
     * Settings#headsPerCall player heads at once.
     *
     * @since 1.14.1
     */
    private void runPlayerHeadGetter() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized (HeadGetter.names)
            {
                int counter = 0;

                while (!HeadGetter.names.isEmpty() && counter < plugin.getSettings().getHeadsPerCall())
                {
                    Pair<String, PanelItem> elementEntry = HeadGetter.names.poll();
                    final String userName = elementEntry.getKey();

                    // Hmm, task in task in task. That is a weird structure.
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        // Check if we can get user Id.
                        UUID userId;

                        if (HeadGetter.cachedHeads.containsKey(userName))
                        {
                            // If cache contains userName, it means that it was already stored.
                            // We can reuse stored data, as they should not be changed.
                            userId = HeadGetter.cachedHeads.get(userName).getUserId();
                        }
                        else if (Bukkit.getServer().getOnlineMode())
                        {
                            // If server is in online mode we can relay that UUID is correct.
                            // So we use thing that is stored in BentoBox players data.
                            userId = plugin.getPlayers().getUUID(userName);
                        }
                        else
                        {
                            // Assign null for later check, as I do not want to write ifs inside
                            // previous 2 checks.
                            userId = null;
                        }

                        HeadCache cache;

                        if (plugin.getSettings().isUseCacheServer())
                        {
                            // Cache server has an implementation to get a skin just from player name.
                            Pair<UUID, String> playerSkin = HeadGetter.getTextureFromName(userName, userId);

                            // Create new cache object.
                            cache = new HeadCache(userName,
                                playerSkin.getKey(),
                                playerSkin.getValue());
                        }
                        else
                        {
                            if (userId == null)
                            {
                                // Use MojangAPI to get userId from userName.
                                userId = HeadGetter.getUserIdFromName(userName);
                            }

                            // Create new cache object.
                            cache = new HeadCache(userName,
                                userId,
                                HeadGetter.getTextureFromUUID(userId));
                        }

                        // Save in cache
                        HeadGetter.cachedHeads.put(userName, cache);

                        // Tell requesters the head came in, but only if the texture is usable.
                        if (cache.encodedTextureLink != null && HeadGetter.headRequesters.containsKey(userName))
                        {
                            for (HeadRequester req : HeadGetter.headRequesters.get(userName))
                            {
                                elementEntry.getValue().setHead(cache.getPlayerHead());

                                if (!plugin.isShutdown())
                                {
                                    // Do not run task if plugin is shutting down.
                                    Bukkit.getScheduler().runTaskAsynchronously(this.plugin,
                                        () -> req.setHead(elementEntry.getValue()));
                                }
                            }
                        }
                    });

                    counter++;
                }
            }
        }, 0, plugin.getSettings().getTicksBetweenCalls());
    }


    /**
     * This method gets and returns userId from mojang web API based on user name.
     * @param name user which Id must be returned.
     * @return String value for user Id.
     * @since 1.14.1
     */
    private static UUID getUserIdFromName(String name) {
        UUID userId;

        try
        {
            Gson gsonReader = new Gson();

            // Get mojang user-id from given nickname
            JsonObject jsonObject = gsonReader.fromJson(
                HeadGetter.getURLContent("https://api.mojang.com/users/profiles/minecraft/" + name),
                JsonObject.class);
            /*
             * Returned Json Object:
              {
                  name: USER_NAME,
                  id: USER_ID
              }
              */

            // Mojang returns ID without `-`. So it is necessary to insert them back.
            // Well technically it is not necessary and can use just a string instead of UUID.
            // UUID just looks more fancy :)
            String userIdString = jsonObject.get("id").toString().
                replace("\"", "").
                replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

            userId = UUID.fromString(userIdString);
        }
        catch (Exception ignored)
        {
            // Return random if failed?
            userId = UUID.randomUUID();
        }

        return userId;
    }


    /**
     * This method gets and returns base64 encoded link to player skin texture, based on
     * given player UUID.
     *
     * @param userId UUID value for the user.
     * @return Encoded player skin texture or null.
     * @since 1.14.1
     */
    private static @Nullable String getTextureFromUUID(UUID userId) {
        try
        {
            Gson gsonReader = new Gson();

            // Get user encoded texture value.
            JsonObject jsonObject = gsonReader.fromJson(
                HeadGetter.getURLContent("https://sessionserver.mojang.com/session/minecraft/profile/" + userId.toString()),
                JsonObject.class);

            /*
             * Returned Json Object:
              {
                id: USER_ID,
                name: USER_NAME,
                properties: [
                    {
                        name: "textures",
                        value: ENCODED_BASE64_TEXTURE
                    }
                 ]
               }
              */

            String decodedTexture = "";

            for (JsonElement element : jsonObject.getAsJsonArray("properties"))
            {
                JsonObject object = element.getAsJsonObject();

                if (object.has("name") &&
                    object.get("name").getAsString().equals("textures"))
                {
                    decodedTexture = object.get("value").getAsString();
                    break;
                }
            }

            return decodedTexture;
        }
        catch (Exception ignored)
        {
        }

        return null;
    }


    /**
     * This method gets and returns base64 encoded link to player skin texture from mc-heads.net.
     * It tries to use UUID if it is a valid, otherwise it uses given username.
     *
     * @param userName userName
     * @param userId UUID for the user.
     * @return Encoded player skin texture or null.
     * @since 1.16.0
     */
    private static @NonNull Pair<UUID, String> getTextureFromName(String userName, @Nullable UUID userId) {
        try
        {
            Gson gsonReader = new Gson();

            // Get user encoded texture value.
            // mc-heads returns correct skin with providing just a name, unlike mojang api, which
            // requires UUID.
            JsonObject jsonObject = gsonReader.fromJson(
                HeadGetter.getURLContent("https://mc-heads.net/minecraft/profile/" + (userId == null ? userName : userId.toString())),
                JsonObject.class);

            /*
             * Returned Json Object:
              {
                id: USER_ID,
                name: USER_NAME,
                properties: [
                    {
                        name: "textures",
                        value: ENCODED_BASE64_TEXTURE
                    }
                 ]
               }
              */

            String decodedTexture = "";

            String userIdString = jsonObject.get("id").toString().
                replace("\"", "").
                replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

            for (JsonElement element : jsonObject.getAsJsonArray("properties"))
            {
                JsonObject object = element.getAsJsonObject();

                if (object.has("name") &&
                    object.get("name").getAsString().equals("textures"))
                {
                    decodedTexture = object.get("value").getAsString();
                    break;
                }
            }

            return new Pair<>(UUID.fromString(userIdString), decodedTexture);
        }
        catch (Exception ignored)
        {
        }

        // return random uuid and null, to assign some values for cache.
        return new Pair<>(userId, null);
    }


    /**
     * This method gets page content of requested url
     *
     * @param requestedUrl Url which content must be returned.
     * @return Content of a page or empty string.
     * @since 1.14.1
     */
    private static String getURLContent(String requestedUrl) {
        String returnValue;

        try
        {
            URL url = new URL(requestedUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            returnValue = br.lines().collect(Collectors.joining());
            br.close();
        }
        catch (Exception e)
        {
            returnValue = "";
        }

        return returnValue;
    }
}
