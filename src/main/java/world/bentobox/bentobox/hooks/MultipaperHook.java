package world.bentobox.bentobox.hooks;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.github.puregero.multilib.DataStorageImpl;
import com.github.puregero.multilib.MultiLib;

import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Hook for Multipaper
 */
public class MultipaperHook extends Hook {

    public MultipaperHook() {
        super("multipaper", Material.PAPER);
    }

    @Override
    public boolean hook() {
        return MultiLib.isMultiPaper();
    }

    /**
     * @return true if this is a Multipaper server
     */
    @Override
    public boolean isPluginAvailable() {
        return MultiLib.isMultiPaper();
    }

    /**
     * Always null because it is not a plugin
     * @return null
     */
    @Nullable
    @Override
    public Plugin getPlugin() {
        return null;
    }

    /**
     * Returns whether the chunk is running on an external server or not.
     *
     * @return True if the chunk is an external chunk, or false if the chunk
     * is running on this server or if it's unloaded.
     */
    public static boolean isChunkExternal(World world, int cx, int cz) {
        return MultiLib.isChunkExternal(world, cx, cz);
    }

    /**
     * Returns whether the chunk is running on an external server or not.
     *
     * @return True if the chunk is an external chunk, or false if the chunk
     * is running on this server or if it's unloaded.
     */
    public static boolean isChunkExternal(Location location) {
        return MultiLib.isChunkExternal(location);
    }

    /**
     * Returns whether the chunk is running on an external server or not.
     *
     * @return True if the chunk is an external chunk, or false if the chunk
     * is running on this server or if it's unloaded.
     */
    public static boolean isChunkExternal(Entity entity) {
        return MultiLib.isChunkExternal(entity);
    }

    /**
     * Returns whether the chunk is running on an external server or not.
     *
     * @return True if the chunk is an external chunk, or false if the chunk
     * is running on this server or if it's unloaded.
     */
    public static boolean isChunkExternal(Block block) {
        return MultiLib.isChunkExternal(block);
    }

    /**
     * Returns whether the chunk is running on an external server or not.
     *
     * @return True if the chunk is an external chunk, or false if the chunk
     * is running on this server or if it's unloaded.
     */
    public static boolean isChunkExternal(Chunk chunk) {
        return MultiLib.isChunkExternal(chunk);
    }

    /**
     * Returns whether the chunk is running on this server or not.
     *
     * @return True if the chunk is a local chunk, or false if the chunk
     * is running on an external server or if it's unloaded.
     */
    public static boolean isChunkLocal(World world, int cx, int cz) {
        return MultiLib.isChunkLocal(world, cx, cz);
    }

    /**
     * Returns whether the chunk is running on this server or not.
     *
     * @return True if the chunk is a local chunk, or false if the chunk
     * is running on an external server or if it's unloaded.
     */
    public static boolean isChunkLocal(Location location) {
        return MultiLib.isChunkLocal(location);
    }

    /**
     * Returns whether the chunk is running on this server or not.
     *
     * @return True if the chunk is a local chunk, or false if the chunk
     * is running on an external server or if it's unloaded.
     */
    public static boolean isChunkLocal(Entity entity) {
        return MultiLib.isChunkLocal(entity);
    }

    /**
     * Returns whether the chunk is running on this server or not.
     *
     * @return True if the chunk is a local chunk, or false if the chunk
     * is running on an external server or if it's unloaded.
     */
    public static boolean isChunkLocal(Block block) {
        return MultiLib.isChunkLocal(block);
    }

    /**
     * Returns whether the chunk is running on this server or not.
     *
     * @return True if the chunk is a local chunk, or false if the chunk
     * is running on an external server or if it's unloaded.
     */
    public static boolean isChunkLocal(Chunk chunk) {
        return MultiLib.isChunkLocal(chunk);
    }

    /**
     * Returns whether the player is on an external server or not.
     *
     * @return True if the player is on an external server.
     */
    public static boolean isExternalPlayer(Player player) {
        return MultiLib.isExternalPlayer(player);
    }

    /**
     * Returns whether the player is on this server or not.
     *
     * @return True if the player is on this server.
     */
    public static boolean isLocalPlayer(Player player) {
        return MultiLib.isLocalPlayer(player);
    }

    /**
     * Get the bungeecord name of this server.
     *
     * @return the bungeecord name of this server
     */
    @NonNull
    public static String getLocalServerName() {
        return MultiLib.getLocalServerName();
    }

    /**
     * Get the bungeecord name of the server that this player is on.
     *
     * @return The bungeecord name of the server the player is on for external
     *         players, or null for local players.
     */
    @Nullable
    public static String getExternalServerName(Player player) {
        return MultiLib.getExternalServerName(player);
    }

    /**
     * Returns cross-server data that is stored under the specified key. Note
     * that all plugins share the same set of keys. This data is
     * non-persistent, it will be lost when the player disconnects.
     *
     * @param key The key the data is stored under.
     * @return The data stored under the key, or null if the key isn't set.
     */
    public static String getData(Player player, String key) {
        return MultiLib.getData(player, key);
    }

    /**
     * Store cross-server data under the specified key. Note that all plugins
     * share the same set of keys. This data is non-persistent, it will be
     * lost when the player disconnects.
     *
     * @param key The key to store the data under.
     * @param value The data to store under the key.
     */
    public static void setData(Player player, String key, String value) {
        MultiLib.setData(player, key, value);
    }

    /**
     * Returns cross-server data that is stored under the specified key. Note
     * that all plugins share the same set of keys. This data is persistent,
     * it will be saved even if the player disconnects. This persistent data is
     * saved onto the player's .dat file.
     *
     * @param key The key the data is stored under.
     * @return The data stored under the key, or null if the key isn't set.
     */
    public static String getPersistentData(Player player, String key) {
        return MultiLib.getPersistentData(player, key);
    }

    /**
     * Store cross-server data under the specified key. Note that all plugins
     * share the same set of keys. This data is persistent, it will be saved
     * even if the player disconnects. This persistent data is saved onto the
     * player's .dat file.
     *
     * @param key The key to store the data under.
     * @param value The data to store under the key.
     */
    public static void setPersistentData(Player player, String key, String value) {
        MultiLib.setPersistentData(player, key, value);
    }

    /**
     * Listen to notifications sent by other servers.
     *
     * @param plugin The plugin listening to these notifications
     * @param channel The notification channel to listen to
     * @param callback A handler for any data received
     */
    public static void on(Plugin plugin, String channel, Consumer<byte[]> callback) {
        MultiLib.on(plugin, channel, callback);
    }

    /**
     * Listen to notifications sent by other servers.
     *
     * @param plugin The plugin listening to these notifications
     * @param channel The notification channel to listen to
     * @param callback A handler for any data received
     */
    public static void onString(Plugin plugin, String channel, Consumer<String> callback) {
        MultiLib.onString(plugin, channel, callback);
    }

    /**
     * Listen to notifications sent by other servers.
     *
     * @param plugin The plugin listening to these notifications
     * @param channel The notification channel to listen to
     * @param callbackWithReply A handler for any data received, and a method to reply to the server on a specified channel
     */
    public static void on(Plugin plugin, String channel,
            BiConsumer<byte[], BiConsumer<String, byte[]>> callbackWithReply) {
        MultiLib.on(plugin, channel, callbackWithReply);
    }

    /**
     * Listen to notifications sent by other servers.
     *
     * @param plugin The plugin listening to these notifications
     * @param channel The notification channel to listen to
     * @param callbackWithReply A handler for any data received, and a method to reply to the server on a specified channel
     */
    public static void onString(Plugin plugin, String channel,
            BiConsumer<String, BiConsumer<String, String>> callbackWithReply) {
        MultiLib.onString(plugin, channel, callbackWithReply);
    }

    /**
     * Notify all other servers.
     *
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notify(String channel, byte[] data) {
        MultiLib.notify(channel, data);
    }

    /**
     * Notify all other servers.
     *
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notify(String channel, String data) {
        MultiLib.notify(channel, data);
    }

    /**
     * Notify other servers with the specified chunk loaded
     *
     * @param chunk The chunk that's loaded
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notify(Chunk chunk, String channel, byte[] data) {
        MultiLib.notify(chunk, channel, data);
    }

    /**
     * Notify other servers with the specified chunk loaded
     *
     * @param chunk The chunk that's loaded
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notify(Chunk chunk, String channel, String data) {
        MultiLib.notify(chunk, channel, data);
    }

    /**
     * Notify the owning server of the specified chunk.
     * This chunk must be loaded on this server.
     * This will notify this server if this server is the owning server.
     *
     * @param chunk The loaded chunk with an owning server
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notifyOwningServer(Chunk chunk, String channel, byte[] data) {
        MultiLib.notifyOwningServer(chunk, channel, data);
    }

    /**
     * Notify the owning server of the specified chunk.
     * This chunk must be loaded on this server.
     * This will notify this server if this server is the owning server.
     *
     * @param chunk The loaded chunk with an owning server
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notifyOwningServer(Chunk chunk, String channel, String data) {
        MultiLib.notifyOwningServer(chunk, channel, data);
    }

    /**
     * Notify the owning server of the specified player.
     * This will notify this server if this server is the owning server.
     *
     * @param player The player with an owning server
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notifyOwningServer(Player player, String channel, byte[] data) {
        MultiLib.notifyOwningServer(player, channel, data);
    }

    /**
     * Notify the owning server of the specified player.
     * This will notify this server if this server is the owning server.
     *
     * @param player The player with an owning server
     * @param channel The notification channel to notify on
     * @param data The data to notify other servers with
     */
    public static void notifyOwningServer(Player player, String channel, String data) {
        MultiLib.notifyOwningServer(player, channel, data);
    }

    /**
     * Says a message (or runs a command) on other servers excluding this one.
     *
     * @param message The chat message to say
     */
    public static void chatOnOtherServers(Player player, String message) {
        MultiLib.chatOnOtherServers(player, message);
    }

    /**
     * Returns all online players across all server instances.
     *
     * @return a view of all online players
     */
    public static Collection<? extends Player> getAllOnlinePlayers() {
        return MultiLib.getAllOnlinePlayers();
    }

    /**
     * Returns players logged into your single local server instance.
     *
     * @return a view of players online on your local instance
     */
    public static Collection<? extends Player> getLocalOnlinePlayers() {
        return MultiLib.getLocalOnlinePlayers();
    }

    /**
     * Gets the multipaper key-value data storage. Accessing this data is
     * asynchronous. This storage medium is hosted on the Master instance,
     * or a yaml file when using Bukkit.
     *
     * @return the multipaper data storage
     */
    public static DataStorageImpl getDataStorage() {
        return MultiLib.getDataStorage();
    }

}
