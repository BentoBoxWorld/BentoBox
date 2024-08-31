package world.bentobox.bentobox.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
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
    private final @NonNull List<Names> nameCache;
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
        nameCache = names.loadObjects();
        inTeleport = new HashSet<>();
    }

    /**
     * Used only for testing. Sets the database to a mock database.
     * @param handler - handler
     */
    public void setHandler(Database<Players> handler) {
        this.handler = handler;
    }

    public void shutdown(){
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
        return uniqueID == null ? false : handler.objectExists(uniqueID.toString());
    }

    /**
     * Attempts to return a UUID for a given player's name.
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
        return nameCache.stream().filter(n -> n.getUniqueId().equalsIgnoreCase(name)).findFirst()
                .map(Names::getUuid).orElse(null);
    }

    /**
     * Sets the player's name and updates the name to UUID database
     * @param user - the User
     */
    public void setPlayerName(@NonNull User user) {
        // Ignore any bots
        if (user.getUniqueId() == null) {
            return;
        }
        Players player = getPlayer(user.getUniqueId());
        player.setPlayerName(user.getName());
        handler.saveObject(player);
        // Update names
        Names newName = new Names(user.getName(), user.getUniqueId());
        // Add to cache
        nameCache.removeIf(name -> user.getUniqueId().equals(name.getUuid()));
        nameCache.add(newName);
        // Add to names database
        names.saveObjectAsync(newName);
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
        handler.saveObject(p);
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
        handler.saveObject(p);
    }

    /**
     * Add death to player
     * @param world - world
     * @param playerUUID - the player's UUID
     */
    public void addDeath(World world, UUID playerUUID) {
        Players p = getPlayer(playerUUID);
        p.addDeath(world);
        handler.saveObject(p);
    }

    /**
     * Set death number for player
     * @param world - world
     * @param playerUUID - the player's UUID
     * @param deaths - number of deaths
     */
    public void setDeaths(World world, UUID playerUUID, int deaths) {
        Players p = getPlayer(playerUUID);
        p.setDeaths(world, deaths);
        handler.saveObject(p);
    }

    /**
     * Get number of times player has died since counting began
     * @param world - world
     * @param playerUUID - the player's UUID
     * @return number of deaths
     */
    public int getDeaths(World world, UUID playerUUID) {
        return getPlayer(playerUUID).getDeaths(world);
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
        handler.saveObject(p);
    }

    /**
     * Remove player from database
     * @param player player to remove
     */
    public void removePlayer(Player player) {
        handler.deleteID(player.getUniqueId().toString());
    }

    /**
     * Cleans the player when leaving an island
     * @param world - island world
     * @param target - target user
     * @param kicked - true if player is being kicked
     * @param island - island being left
     * @since 1.15.4
     */
    public void cleanLeavingPlayer(World world, User target, boolean kicked, Island island) {
        // Execute commands when leaving
        String ownerName = this.getName(island.getOwner());
        Util.runCommands(target, ownerName, plugin.getIWM().getOnLeaveCommands(world), "leave");

        // Remove any tamed animals
        world.getEntitiesByClass(Tameable.class).stream()
        .filter(Tameable::isTamed)
        .filter(t -> t.getOwner() != null && t.getOwner().getUniqueId().equals(target.getUniqueId()))
        .forEach(t -> t.setOwner(null));

        // Remove money inventory etc.
        if (plugin.getIWM().isOnLeaveResetEnderChest(world)) {
            if (target.isOnline()) {
                target.getPlayer().getEnderChest().clear();
            } else {
                Players p = getPlayer(target.getUniqueId());
                if (p != null) {
                    p.addToPendingKick(world);
                }
            }
        }
        if ((kicked && plugin.getIWM().isOnLeaveResetInventory(world) && !plugin.getIWM().isKickedKeepInventory(world))
                || (!kicked && plugin.getIWM().isOnLeaveResetInventory(world))) {
            if (target.isOnline()) {
                target.getPlayer().getInventory().clear();
            } else {
                Players p = getPlayer(target.getUniqueId());
                if (p != null) {
                    p.addToPendingKick(world);
                }
            }
        }

        if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnLeaveResetMoney(world)) {
            plugin.getVault().ifPresent(vault -> vault.withdraw(target, vault.getBalance(target), world));
        }
        // Reset the health
        if (plugin.getIWM().isOnLeaveResetHealth(world) && target.isPlayer()) {
            Util.resetHealth(target.getPlayer());
        }

        // Reset the hunger
        if (plugin.getIWM().isOnLeaveResetHunger(world) && target.isPlayer()) {
            target.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (plugin.getIWM().isOnLeaveResetXP(world) && target.isPlayer()) {
            // Player collected XP (displayed)
            target.getPlayer().setLevel(0);
            target.getPlayer().setExp(0);
            // Player total XP (not displayed)
            target.getPlayer().setTotalExperience(0);
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

}
