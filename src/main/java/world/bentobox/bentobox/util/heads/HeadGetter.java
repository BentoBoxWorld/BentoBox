package world.bentobox.bentobox.util.heads;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;


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
    private static final Map<String, PanelItem> names = new HashMap<>();

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

        // Get value from config. Multiply value to 60 000 as internally it uses miliseconds.
        // Config value stores minutes.
        long cacheTimeout = BentoBox.getInstance().getSettings().getPlayerHeadCacheTime() * 60 * 1000;

        // to avoid every time clearing stored heads (as they may become very large)
        // just check if requested cache exists and compare it with value from plugin settings.
        // If timestamp is set to 0, then it must be kept forever.
        // If settings time is set to 0, then always use cache.
        if (cache != null &&
            cache.getTimestamp() != 0 &&
            cacheTimeout > 0 &&
            System.currentTimeMillis() - cache.getTimestamp() <= cacheTimeout)
        {
            panelItem.setHead(cachedHeads.get(panelItem.getPlayerHeadName()).getPlayerHead());
            requester.setHead(panelItem);
        }
        else
        {
            // Get the name
            headRequesters.computeIfAbsent(panelItem.getPlayerHeadName(), k -> new HashSet<>()).
                add(requester);
            names.put(panelItem.getPlayerHeadName(), panelItem);
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
     * This is main task that runs once every 20 ticks and tries to get a player head.
     * @since 1.14.1
     */
    private void runPlayerHeadGetter() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized (names)
            {
                Iterator<Entry<String, PanelItem>> it = names.entrySet().iterator();

                if (it.hasNext())
                {
                    Entry<String, PanelItem> elementEntry = it.next();

                    // TODO: In theory BentoBox could use User instance to find existing user UUID's.
                    // It would avoid one API call.
                    final String userName = elementEntry.getKey();

                    // Use cached userId as userId will not change :)
                    UUID userId = cachedHeads.containsKey(userName) ?
                        cachedHeads.get(userName).getUserId() :
                        HeadGetter.getUserIdFromName(userName);

                    // Create new cache object.
                    HeadCache cache = new HeadCache(userName,
                        userId,
                        HeadGetter.getTextureFromUUID(userId));

                    // Save in cache
                    cachedHeads.put(userName, cache);

                    // Tell requesters the head came in
                    if (headRequesters.containsKey(userName))
                    {
                        for (HeadRequester req : headRequesters.get(userName))
                        {
                            elementEntry.getValue().setHead(cache.getPlayerHead());

                            Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin,
                                () -> req.setHead(elementEntry.getValue()));
                        }
                    }

                    it.remove();
                }
            }
        }, 0L, 20L);
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
                replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
                    "$1-$2-$3-$4-$5");

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
     * This method gets page content of requested url
     *
     * @param requestedUrl Url which content must be returned.
     * @return Content of a page or empty string.
     * @since 1.14.1
     */
    private static String getURLContent(String requestedUrl) {
        String returnValue;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new URL(requestedUrl).openStream(), StandardCharsets.UTF_8)))
        {
            returnValue = reader.lines().collect(Collectors.joining());
        }
        catch (Exception ignored)
        {
            returnValue = "";
        }

        return returnValue;
    }
}