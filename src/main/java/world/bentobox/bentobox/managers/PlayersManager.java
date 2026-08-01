package world.bentobox.bentobox.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.player.PlayerEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.util.ExpiringMap;
import world.bentobox.bentobox.util.Util;

public class PlayersManager {

    private final BentoBox plugin;
    private Database<Players> handler;
    private final Database<Names> names;
    private final ExpiringMap<UUID, Players> playerCache = new ExpiringMap<>(2, TimeUnit.HOURS);
    /**
     * Name to UUID lookup, keyed on the lower cased name so that lookups never depend
     * on how the name happened to be capitalized when it was stored. Minecraft names are
     * unique ignoring case, so the key is unambiguous.
     * <p>
     * This table is the whole reason {@link #getUUID(String)} can be called from a command:
     * it is loaded once at startup and kept current as players join, so a lookup never has
     * to hit the database. Keep {@code getUUID} free of blocking calls.
     */
    private final @NonNull Map<String, Names> nameCache = new ConcurrentHashMap<>();
    private final Set<UUID> inTeleport; // this needs databasing

    /**
     * Provides a memory cache of online player information
     * This is the one-stop-shop of player info
     * If the player is not cached, then a request is made to Players to obtain it
     *
     * @param plugin - plugin object
     */
    public PlayersManager(BentoBox plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Players classes
        handler = new Database<>(plugin, Players.class);
        // Set up the names database
        names = new Database<>(plugin, Names.class);
        names.loadObjects().forEach(this::cacheName);
        inTeleport = new HashSet<>();
    }

    /**
     * Key used for name lookups. Names are matched ignoring case throughout.
     */
    private static String nameKey(@NonNull String name) {
        return name.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Puts a name record into the lookup cache, ignoring records that cannot be used.
     */
    private void cacheName(@Nullable Names name) {
        if (name != null && name.getUuid() != null && name.getUniqueId() != null
                && !name.getUniqueId().isEmpty()) {
            nameCache.put(nameKey(name.getUniqueId()), name);
        }
    }

    /**
     * Used only for testing. Sets the database to a mock database.
     * @param handler - handler
     */
    public void setHandler(Database<Players> handler) {
        this.handler = handler;
    }

    public void shutdown(){
        // Save all players in cache
        playerCache.forEach((uuid, player) -> handler.saveObjectAsync(player));
        handler.close();
        playerCache.shutdown();
    }

    /**
     * Get player by UUID. Adds player to cache if not in there already
     * @param uuid of player
     * @return player object or null if it does not exist, for example the UUID is null
     */
    @Nullable
    public Players getPlayer(UUID uuid){
        return playerCache.computeIfAbsent(uuid, this::addPlayer);
    }

    /**
     * Adds a player to the database. If the UUID does not exist, a new player is created.
     *
     * @param playerUUID the player's UUID, must not be null
     * @return the loaded or newly created player
     * @throws NullPointerException if playerUUID is null
     */
    private Players addPlayer(@NonNull UUID playerUUID) {
        Objects.requireNonNull(playerUUID, "Player UUID must not be null");

        // If the player exists in the database, load it; otherwise, create and save a new player
        if (handler.objectExists(playerUUID.toString())) {
            Players p = loadPlayer(playerUUID);
            if (p != null) {
                return p;
            }
        }
        Players newPlayer = new Players(plugin, playerUUID);
        handler.saveObjectAsync(newPlayer);
        return newPlayer;
    }

    /**
     * Force load the player from the database. The player must be known to BenoBox. If it is not
     * use {@link #addPlayer(UUID)} instead. This is a blocking call, so be careful.
     * @param uuid UUID of player
     * @return Players object representing that player
     * @since 2.4.0
     */
    public @Nullable Players loadPlayer(UUID uuid) {
        return handler.loadObject(uuid.toString());
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the players that are <strong>currently in the cache</strong>.
     * @return unmodifiable collection containing every player in the cache.
     * @since 1.1
     */
    @NonNull
    public Collection<Players> getPlayers() {
        return Collections.unmodifiableCollection(handler.loadObjects());
    }

    /**
     * Checks if the player is known or not.
     * Will check not just the cache but if the object but in the database too.
     *
     * @param uniqueID - unique ID
     * @return true if player is known, otherwise false
     */
    public boolean isKnown(UUID uniqueID) {
        return uniqueID != null && handler.objectExists(uniqueID.toString());
    }

    /**
     * Attempts to return a UUID for a given player's name.
     * <p>
     * Names are matched ignoring case: Minecraft names are unique ignoring case, so
     * {@code Oli713664} and {@code oli713664} are the same player and both must resolve.
     *
     * @param name - name of player
     * @return UUID of player or null if unknown
     */
    @Nullable
    public UUID getUUID(@NonNull String name) {
        // See if this is a UUID
        // example: 5988eecd-1dcd-4080-a843-785b62419abb
        if (name.length() == 36 && name.contains("-")) {
            try {
                return UUID.fromString(name);
            } catch (Exception ignored) {
                // Not used
            }
        }
        if (name.isBlank()) {
            return null;
        }
        // Every step below is an in-memory lookup. The Names table exists precisely so that
        // resolving a name never blocks the calling thread, so nothing here may touch the
        // database or the network - see the class comment on nameCache.

        // An online player is authoritative. Bukkit matches the name ignoring case, and this
        // beats anything stored, which may be a leftover record from a previous holder of the
        // name or predate a rename.
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        Names cached = nameCache.get(nameKey(name));
        if (cached != null) {
            return cached.getUuid();
        }
        // Last resort: the server's own user cache, which knows players BentoBox has never
        // seen. Unlike the deprecated getOfflinePlayer(String), this never makes a web
        // request - it returns null on a miss rather than going looking.
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        return offline == null ? null : offline.getUniqueId();
    }

    /**
     * Sets the player's name and updates the name to UUID database
     * @param user - the User
     * @return CompletableFuture true if saved, false if not
     */
    public CompletableFuture<Boolean> setPlayerName(@NonNull User user) {
        // Ignore any bots
        if (user.getUniqueId() == null) {
            return CompletableFuture.completedFuture(false);
        }
        Players player = getPlayer(user.getUniqueId());
        player.setPlayerName(user.getName());
        handler.saveObjectAsync(player);
        // Update names
        Names newName = new Names(user.getName(), user.getUniqueId());
        // Drop any record that still points at this player under some other spelling - an old
        // name, or this name stored with different capitalization. Left in place it keeps
        // resolving to a UUID that is no longer correct for that name, and on a case sensitive
        // file system it shadows the record written below.
        for (Iterator<Names> it = nameCache.values().iterator(); it.hasNext();) {
            Names old = it.next();
            if (user.getUniqueId().equals(old.getUuid()) && !user.getName().equals(old.getUniqueId())) {
                names.deleteID(old.getUniqueId());
                it.remove();
            }
        }
        // Add to cache
        cacheName(newName);
        // Add to names database
        return names.saveObjectAsync(newName);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     *
     * @param playerUUID - the player's UUID
     * @return String - playerName, empty string if UUID is null
     */
    @NonNull
    public String getName(@Nullable UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        getPlayer(playerUUID);
        return Objects.requireNonNullElse(playerCache.get(playerUUID).getPlayerName(), "");
    }

    /**
     * Returns how many island resets the player has done.
     * @param world world
     * @param playerUUID the player's UUID
     * @return number of resets
     */
    public int getResets(World world, UUID playerUUID) {
        return getPlayer(playerUUID).getResets(world);
    }

    /**
     * Returns how many island resets the player can still do.
     * @param world world
     * @param playerUUID the player's UUID
     * @return number of resets the player can do (always {@code >= 0}), or {@code -1} if unlimited.
     * @since 1.5.0
     * @see #getResets(World, UUID)
     */
    public int getResetsLeft(World world, UUID playerUUID) {
        getPlayer(playerUUID);
        if (plugin.getIWM().getResetLimit(world) == -1) {
            return -1;
        } else {
            return Math.max(plugin.getIWM().getResetLimit(world) - getResets(world, playerUUID), 0);
        }
    }

    /**
     * Sets how many resets the player has performed
     *
     * @param world world
     * @param playerUUID player's UUID
     * @param resets number of resets to set
     */
    public void setResets(World world, UUID playerUUID, int resets) {
        Players p = getPlayer(playerUUID);
        p.setResets(world, resets);
        handler.saveObjectAsync(p);
    }

    /**
     * Returns the locale for this player. If missing, will return nothing
     * @param playerUUID - the player's UUID
     * @return name of the locale this player uses
     */
    public String getLocale(UUID playerUUID) {
        return getPlayer(playerUUID).getLocale();
    }

    /**
     * Sets the locale this player wants to use
     * @param playerUUID - the player's UUID
     * @param localeName - locale name, e.g., en-US
     */
    public void setLocale(UUID playerUUID, String localeName) {
        Players p = getPlayer(playerUUID);
        p.setLocale(localeName);
        handler.saveObjectAsync(p);
    }

    /**
     * Add death to player
     * @param world - world (this includes any nether or end)
     * @param playerUUID - the player's UUID
     */
    public void addDeath(World world, UUID playerUUID) {
        Players p = getPlayer(playerUUID);
        p.addDeath(Util.getWorld(world));
        handler.saveObjectAsync(p);
    }

    /**
     * Set death number for player
     * @param world - world (this includes any nether or end)
     * @param playerUUID - the player's UUID
     * @param deaths - number of deaths
     */
    public void setDeaths(World world, UUID playerUUID, int deaths) {
        Players p = getPlayer(playerUUID);
        p.setDeaths(Util.getWorld(world), deaths);
        handler.saveObjectAsync(p);
    }

    /**
     * Get number of times player has died since counting began
     * @param world - world (this includes any nether or end)
     * @param playerUUID - the player's UUID
     * @return number of deaths
     */
    public int getDeaths(World world, UUID playerUUID) {
        return getPlayer(playerUUID).getDeaths(Util.getWorld(world));
    }

    /**
     * Sets if a player is mid-teleport or not
     * @param uniqueId - unique ID
     */
    public void setInTeleport(UUID uniqueId) {
        inTeleport.add(uniqueId);
    }

    /**
     * Removes player from in-teleport
     * @param uniqueId - unique ID
     */
    public void removeInTeleport(UUID uniqueId) {
        inTeleport.remove(uniqueId);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if a player is mid-teleport
     */
    public boolean isInTeleport(UUID uniqueId) {
        return inTeleport.contains(uniqueId);
    }

    /**
     * Tries to get the user from his name
     * @param name - name
     * @return user - user or null if unknown
     */
    public User getUser(String name) {
        UUID uuid = getUUID(name);
        return uuid == null ? null : getUser(uuid);
    }

    /**
     * Tries to get the user from his UUID
     * @param uuid - UUID
     * @return user - user
     */
    public User getUser(UUID uuid) {
        return User.getInstance(uuid);
    }

    /**
     * Adds a reset to this player's number of resets
     * @param world world where island is
     * @param playerUUID player's UUID
     */
    public void addReset(World world, UUID playerUUID) {
        Players p = getPlayer(playerUUID);
        p.addReset(world);
        handler.saveObjectAsync(p);
    }

    /**
     * Remove player from database
     * @param player player to remove
     */
    public void removePlayer(Player player) {
        handler.deleteID(player.getUniqueId().toString());
        // Drop the name lookup too, otherwise the name keeps resolving to a player that
        // no longer has any data.
        for (Iterator<Names> it = nameCache.values().iterator(); it.hasNext();) {
            Names name = it.next();
            if (player.getUniqueId().equals(name.getUuid())) {
                names.deleteID(name.getUniqueId());
                it.remove();
            }
        }
    }

    /**
     * Cleans the player when leaving an island.
     * <p>
     * For each configurable reset action a cancellable {@link world.bentobox.bentobox.api.events.player.PlayerBaseEvent}
     * is fired via {@link PlayerEvent#builder()} before the action is executed. If the event is
     * cancelled by a listener the corresponding action is skipped entirely. The events fired, in order, are:
     * <ol>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerTamedRemovalEvent} – before untaming the player's animals</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetEnderChestEvent} – before clearing the ender chest</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetInventoryEvent} – before clearing the inventory</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetMoneyEvent} – before withdrawing the player's balance</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetHealthEvent} – before resetting health</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetHungerEvent} – before resetting hunger</li>
     *   <li>{@link world.bentobox.bentobox.api.events.player.PlayerResetExpEvent} – before resetting XP</li>
     * </ol>
     *
     * @param world  the island world
     * @param target the target user
     * @param kicked {@code true} if the player is being kicked from the team
     * @param island the island being left
     * @since 1.15.4
     */
    public void cleanLeavingPlayer(World world, User target, boolean kicked, Island island) {
        // Execute on-leave commands unconditionally (not a player-state reset, no event needed)
        String ownerName = this.getName(island.getOwner());
        Util.runCommands(target, ownerName, plugin.getIWM().getOnLeaveCommands(world), "leave");

        // Remove any tamed animals – skipped if the TAMED_REMOVAL event is cancelled
        if (!PlayerEvent.builder()
                .world(world).island(island).involvedPlayer(target.getUniqueId())
                .reason(PlayerEvent.Reason.TAMED_REMOVAL).build().isCancelled()) {
            world.getEntitiesByClass(Tameable.class).stream()
                    .filter(Tameable::isTamed)
                    .filter(t -> t.getOwner() != null && t.getOwner().getUniqueId().equals(target.getUniqueId()))
                    .forEach(t -> t.setOwner(null));
        }

        // Clear ender chest – skipped if the ENDERCHEST_RESET event is cancelled
        if (plugin.getIWM().isOnLeaveResetEnderChest(world)) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.ENDERCHEST_RESET).build().isCancelled()) {
                if (target.isOnline()) {
                    target.getPlayer().getEnderChest().clear();
                } else {
                    Players p = getPlayer(target.getUniqueId());
                    if (p != null) {
                        p.addToPendingKick(world);
                    }
                }
            }
        }

        // Clear inventory – skipped if the INVENTORY_RESET event is cancelled
        if ((kicked && plugin.getIWM().isOnLeaveResetInventory(world) && !plugin.getIWM().isKickedKeepInventory(world))
                || (!kicked && plugin.getIWM().isOnLeaveResetInventory(world))) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.INVENTORY_RESET).build().isCancelled()) {
                if (target.isOnline()) {
                    target.getPlayer().getInventory().clear();
                } else {
                    Players p = getPlayer(target.getUniqueId());
                    if (p != null) {
                        p.addToPendingKick(world);
                    }
                }
            }
        }

        // Withdraw money – skipped if the MONEY_RESET event is cancelled
        if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnLeaveResetMoney(world)) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.MONEY_RESET).build().isCancelled()) {
                plugin.getVault().ifPresent(vault -> vault.withdraw(target, vault.getBalance(target), world));
            }
        }

        // Reset health – skipped if the HEALTH_RESET event is cancelled
        if (plugin.getIWM().isOnLeaveResetHealth(world) && target.isPlayer()) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.HEALTH_RESET).build().isCancelled()) {
                Util.resetHealth(target.getPlayer());
            }
        }

        // Reset hunger – skipped if the HUNGER_RESET event is cancelled
        if (plugin.getIWM().isOnLeaveResetHunger(world) && target.isPlayer()) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.HUNGER_RESET).build().isCancelled()) {
                target.getPlayer().setFoodLevel(20);
            }
        }

        // Reset XP – skipped if the EXP_RESET event is cancelled
        if (plugin.getIWM().isOnLeaveResetXP(world) && target.isPlayer()) {
            if (!PlayerEvent.builder()
                    .world(world).island(island).involvedPlayer(target.getUniqueId())
                    .reason(PlayerEvent.Reason.EXP_RESET).build().isCancelled()) {
                // Player collected XP (displayed)
                target.getPlayer().setLevel(0);
                target.getPlayer().setExp(0);
                // Player total XP (not displayed)
                target.getPlayer().setTotalExperience(0);
            }
        }
    }

    /**
     * Saves the player async to the database. The player has to be known to BentoBox to be saved.
     * Players are usually detected by BentoBox when they join the server, so this is not an issue.
     * @param uuid UUID of the player
     * @return Completable future true when done, or false if not saved for some reason, e.g., invalid UUID
     * @since 2.4.0
     */
    public CompletableFuture<Boolean> savePlayer(UUID uuid) {
        Players p = this.getPlayer(uuid);
        if (p != null) {
            return handler.saveObjectAsync(p);
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Records when the user last logged in. Called by the joinleave listener
     * @param user user
     * @since 2.7.0
     */
    public void setLoginTimeStamp(User user) {
        if (user.isPlayer() && user.isOnline()) {
            setLoginTimeStamp(user.getUniqueId(), System.currentTimeMillis());
        }
    }

    /**
     * Set the player's last login time to a timestamp
     * @param playerUUID player UUID
     * @param timestamp timestamp to set
     * @since 2.7.0
     */
    public void setLoginTimeStamp(UUID playerUUID, long timestamp) {
        Players p = this.getPlayer(playerUUID);
        if (p != null) {
            p.setLastLogin(timestamp);
            this.savePlayer(playerUUID);
        }
    }

    /**
     * Get the last login time stamp for this player
     * @param uuid player's UUID
     * @return timestamp or null if unknown or not recorded yet
     * @since 2.7.0
     */
    @Nullable
    public Long getLastLoginTimestamp(UUID uuid) {
        Players p = this.getPlayer(uuid);
        if (p != null) {
            return p.getLastLogin();
        }
        return null;
    }

}
