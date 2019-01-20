package world.bentobox.bentobox.managers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.hooks.MultiverseCoreHook;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles registration and management of worlds
 *
 * @author tastybento
 */
public class IslandWorldManager {

    private BentoBox plugin;
    /**
     * Map associating Worlds (Overworld, Nether and End) with the GameModeAddon that creates them.
     */
    private Map<@NonNull World, @NonNull GameModeAddon> gameModes;

    /**
     * Manages worlds registered with BentoBox
     */
    public IslandWorldManager(BentoBox plugin) {
        this.plugin = plugin;
        gameModes = new HashMap<>();
    }

    public void registerWorldsToMultiverse() {
        gameModes.values().stream().distinct().forEach(gm -> {
            registerToMultiverse(gm.getOverWorld());
            if (gm.getWorldSettings().isNetherGenerate() && gm.getWorldSettings().isNetherIslands()) {
                registerToMultiverse(gm.getNetherWorld());
            }
            if (gm.getWorldSettings().isEndGenerate() && gm.getWorldSettings().isEndIslands()) {
                registerToMultiverse(gm.getEndWorld());
            }
        });
    }

    /**
     * Registers a world with Multiverse if Multiverse is available.
     *
     * @param world the World to register
     */
    private void registerToMultiverse(@NonNull World world) {
        if (!isUseOwnGenerator(world) && plugin.getHooks() != null) {
            plugin.getHooks().getHook("Multiverse-Core").ifPresent(hook -> {
                if (Bukkit.isPrimaryThread()) {
                    ((MultiverseCoreHook) hook).registerWorld(world);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> ((MultiverseCoreHook) hook).registerWorld(world));
                }
            });
        }
    }

    /**
     * Checks if a location is in any of the island worlds.
     * This will be false if the player is in the standard Nether or standard End.
     *
     * @param loc - location
     * @return true if in a world or false if not. False if in standard Nether or standard End.
     */
    public boolean inWorld(Location loc) {
        return inWorld(loc.getWorld());
    }

    /**
     * Checks if a world is any of the island worlds
     *
     * @param world world
     * @return true if in a world or false if not
     */
    public boolean inWorld(World world) {
        return ((world.getEnvironment().equals(Environment.NETHER) && isIslandNether(world))
                || (world.getEnvironment().equals(Environment.THE_END) && isIslandEnd(world))
                || (world.getEnvironment().equals(Environment.NORMAL)) && gameModes.containsKey(world));
    }

    /**
     * @return Set of all worlds registered by GameModeAddons
     * @since 1.1
     */
    @NonNull
    public Set<World> getWorlds() {
        return gameModes.keySet();
    }

    /**
     * @return List of over worlds
     */
    public List<World> getOverWorlds() {
        return gameModes.keySet().stream().filter(w -> w.getEnvironment().equals(Environment.NORMAL))
                .collect(Collectors.toList());
    }

    /**
     * Get friendly names of all the over worlds and associated GameModeAddon
     *
     * @return Map of world names and associated GameModeAddon friendly name
     */
    public Map<String, String> getOverWorldNames() {
        return gameModes.values().stream()
                .distinct()
                .collect(Collectors.toMap(a -> a.getOverWorld().getName(), a -> a.getWorldSettings().getFriendlyName()));
    }

    /**
     * Check if a name is a known friendly world name, ignores case
     *
     * @param name - world name
     * @return true if name is a known world name
     */
    public boolean isKnownFriendlyWorldName(String name) {
        return gameModes.values().stream().distinct()
                .anyMatch(gm -> gm.getWorldSettings().getFriendlyName().equalsIgnoreCase(name));
    }


    /**
     * Adds a GameMode to island world manager
     * @param gameMode
     */
    public void addGameMode(GameModeAddon gameMode) {
        WorldSettings settings = gameMode.getWorldSettings();
        World world = gameMode.getOverWorld();
        String friendlyName = settings.getFriendlyName().isEmpty() ? world.getName() : settings.getFriendlyName();
        // Add worlds to map
        gameModes.put(world, gameMode);
        // Call Multiverse
        registerToMultiverse(world);
        if (settings.isNetherGenerate()) {
            gameModes.put(gameMode.getNetherWorld(), gameMode);
            if (settings.isNetherIslands()) {
                registerToMultiverse(gameMode.getNetherWorld());
            }
        }
        if (settings.isEndGenerate() && settings.isEndIslands()) {
            gameModes.put(gameMode.getEndWorld(), gameMode);
            if (settings.isEndGenerate()) {
                registerToMultiverse(gameMode.getEndWorld());
            }
        }
        // Set default island settings
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.PROTECTION))
        .forEach(f -> settings.getDefaultIslandFlags().putIfAbsent(f, f.getDefaultRank()));
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.SETTING))
        .forEach(f -> settings.getDefaultIslandSettings().putIfAbsent(f, f.getDefaultRank()));
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Set world difficulty
            Difficulty diff = settings.getDifficulty();
            if (diff == null) {
                diff = Difficulty.NORMAL;
                settings.setDifficulty(diff);
            }
            world.setDifficulty(diff);

            // Handle nether and end difficulty levels
            if (settings.isNetherGenerate()) {
                this.getNetherWorld(world).setDifficulty(diff);
            }
            if (settings.isEndGenerate()) {
                this.getEndWorld(world).setDifficulty(diff);
            }
            plugin.log("Added world " + friendlyName + " (" + world.getDifficulty() + ")");
        });

    }

    /**
     * Get the settings for this world or sub-worlds (nether, end)
     *
     * @param world
     *            - world
     * @return world settings, or null if world is unknown
     */
    public WorldSettings getWorldSettings(World world) {
        if (gameModes.containsKey(world)) {
            return gameModes.get(world).getWorldSettings();
        } else {
            return null;
        }
    }

    /**
     * Get the overworld based on friendly name.
     *
     * @param friendlyName friendly name of world
     * @return overworld, or null if it does not exist
     * @since 1.1
     */
    @Nullable
    public World getOverWorld(@NonNull String friendlyName) {
        return gameModes.values().stream().distinct()
                .filter(gm -> gm.getWorldSettings().getFriendlyName().equalsIgnoreCase(friendlyName))
                .map(GameModeAddon::getOverWorld).findFirst().orElse(null);
    }

    /**
     * @return the islandDistance
     */
    public int getIslandDistance(World world) {
        return gameModes.get(world).getWorldSettings().getIslandDistance();
    }

    /**
     * @return the islandHeight
     */
    public int getIslandHeight(World world) {
        return gameModes.get(world).getWorldSettings().getIslandHeight();
    }

    /**
     * @return the islandProtectionRange
     */
    public int getIslandProtectionRange(World world) {
        return gameModes.get(world).getWorldSettings().getIslandProtectionRange();
    }

    /**
     * @return the islandStartX
     */
    public int getIslandStartX(World world) {
        return gameModes.get(world).getWorldSettings().getIslandStartX();
    }

    /**
     * @return the islandStartZ
     */
    public int getIslandStartZ(World world) {
        return gameModes.get(world).getWorldSettings().getIslandStartZ();
    }

    /**
     * @return the islandXOffset
     */
    public int getIslandXOffset(World world) {
        return gameModes.get(world).getWorldSettings().getIslandXOffset();
    }

    /**
     * @return the islandZOffset
     */
    public int getIslandZOffset(World world) {
        return gameModes.get(world).getWorldSettings().getIslandZOffset();
    }

    /**
     * @return the maxIslands
     */
    public int getMaxIslands(World world) {
        return gameModes.get(world).getWorldSettings().getMaxIslands();
    }

    /**
     * @return the netherSpawnRadius
     */
    public int getNetherSpawnRadius(World world) {
        return gameModes.get(world).getWorldSettings().getNetherSpawnRadius();
    }

    /**
     * @return the seaHeight
     */
    public int getSeaHeight(World world) {
        return gameModes.get(world).getWorldSettings().getSeaHeight();
    }

    /**
     * @return the worldName
     */
    public String getWorldName(World world) {
        return gameModes.get(world).getWorldSettings().getWorldName();
    }

    /**
     * @return the endGenerate
     */
    public boolean isEndGenerate(World world) {
        return gameModes.get(world).getWorldSettings().isEndGenerate();
    }

    /**
     * @return the endIslands
     */
    public boolean isEndIslands(World world) {
        return gameModes.get(world).getWorldSettings().isEndIslands();
    }

    /**
     * @return the netherGenerate
     */
    public boolean isNetherGenerate(World world) {
        return gameModes.get(world).getWorldSettings().isNetherGenerate();
    }

    /**
     * @return the netherIslands
     */
    public boolean isNetherIslands(World world) {
        return gameModes.get(world).getWorldSettings().isNetherIslands();
    }

    /**
     * Checks if a world is a known nether world
     *
     * @param world - world
     * @return true if world is a known and valid nether world
     */
    public boolean isNether(World world) {
        return world.getEnvironment().equals(Environment.NETHER) && gameModes.containsKey(world) && gameModes.get(world).getWorldSettings().isNetherGenerate();
    }

    /**
     * Checks if a world is a known island nether world
     *
     * @param world - world
     * @return true if world is a known and valid nether world
     */
    public boolean isIslandNether(World world) {
        return world.getEnvironment().equals(Environment.NETHER) && gameModes.containsKey(world) && gameModes.get(world).getWorldSettings().isNetherGenerate()
                && gameModes.get(world).getWorldSettings().isNetherIslands();
    }

    /**
     * Checks if a world is a known end world
     *
     * @param world - world
     * @return true if world is a known and valid end world
     */
    public boolean isEnd(World world) {
        return world.getEnvironment().equals(Environment.THE_END) && gameModes.containsKey(world) && gameModes.get(world).getWorldSettings().isEndGenerate();
    }

    /**
     * Checks if a world is a known island end world
     *
     * @param world
     *            - world
     * @return true if world is a known and valid nether world
     */
    public boolean isIslandEnd(World world) {
        return world.getEnvironment().equals(Environment.THE_END) && gameModes.containsKey(world) && gameModes.get(world).getWorldSettings().isEndGenerate()
                && gameModes.get(world).getWorldSettings().isEndIslands();
    }

    /**
     * Get the nether world of this overWorld
     *
     * @param overWorld - overworld
     * @return nether world, or null if it does not exist
     */
    public World getNetherWorld(World overWorld) {
        if (gameModes.containsKey(overWorld)) {
            return gameModes.get(overWorld).getNetherWorld();
        }
        return null;
    }

    /**
     * Get the end world of this overWorld
     *
     * @param overWorld - overworld
     * @return end world, or null if it does not exist
     */
    public World getEndWorld(World overWorld) {
        if (gameModes.containsKey(overWorld)) {
            return gameModes.get(overWorld).getEndWorld();
        }
        return null;
    }

    /**
     * Check if nether trees should be created in the nether or not
     *
     * @param world - world
     * @return true or false
     */
    public boolean isNetherTrees(World world) {
        return gameModes.containsKey(world) && gameModes.get(world).getWorldSettings().isNetherTrees();
    }

    /**
     * Whether the End Dragon can spawn or not in this world
     *
     * @param world
     *            - world
     * @return true (default) if it can spawn or not
     */
    public boolean isDragonSpawn(World world) {
        return !gameModes.containsKey(world) || gameModes.get(world).getWorldSettings().isDragonSpawn();
    }

    /**
     * @return a comma separated string of friendly world names
     */
    public String getFriendlyNames() {
        StringBuilder r = new StringBuilder();
        gameModes.values().stream().distinct().forEach(n -> r.append(n).append(", "));
        if (r.length() > 0) {
            r.setLength(r.length() - 2);
        }
        return r.toString();
    }

    /**
     * Gets world from friendly name
     *
     * @param friendlyWorldName
     *            - friendly world name. Used for commands.
     * @return world, or null if not known
     */
    public World getIslandWorld(String friendlyWorldName) {
        return gameModes.entrySet().stream().filter(e -> e.getValue().getWorldSettings().getFriendlyName().equalsIgnoreCase(friendlyWorldName)).findFirst()
                .map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Get max team size for this world
     *
     * @param world
     *            - world
     * @return max team size
     */
    public int getMaxTeamSize(World world) {
        return gameModes.get(world).getWorldSettings().getMaxTeamSize();
    }

    /**
     * Get max homes for world
     *
     * @param world
     *            - world
     * @return max homes
     */
    public int getMaxHomes(World world) {
        return gameModes.get(world).getWorldSettings().getMaxHomes();
    }

    /**
     * Get the friendly name for world or related nether or end
     *
     * @param world
     *            - world
     * @return Friendly name
     */
    public String getFriendlyName(World world) {
        return gameModes.get(world).getWorldSettings().getFriendlyName();
    }

    /**
     * Get the permission prefix for this world. No trailing dot included.
     *
     * @param world
     *            - world
     * @return permission prefix for this world
     */
    public String getPermissionPrefix(World world) {
        return gameModes.get(world).getWorldSettings().getPermissionPrefix();

    }

    /**
     * Get the invincible visitor settings for this world
     *
     * @param world
     *            - world
     * @return invincible visitor settings
     */
    public List<String> getIvSettings(World world) {
        return gameModes.get(world).getWorldSettings().getIvSettings();
    }

    /**
     * Returns whether a world flag is set or not Same result as calling
     * {@link Flag#isSetForWorld(World)}
     *
     * @param world
     *            - world
     * @param flag
     *            - world setting flag
     * @return true or false
     */
    public boolean isWorldFlag(World world, Flag flag) {
        return flag.isSetForWorld(world);
    }

    /**
     * Get the default game mode for this world.
     *
     * @param world
     *            - world
     * @return GameMode: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
     */
    public GameMode getDefaultGameMode(World world) {
        return gameModes.get(world).getWorldSettings().getDefaultGameMode();
    }

    /**
     * Get the set of entity types not to remove when player teleports to island
     *
     * @param world
     *            - world
     * @return - set of entity types
     */
    public Set<EntityType> getRemoveMobsWhitelist(World world) {
        return gameModes.get(world).getWorldSettings().getRemoveMobsWhitelist();
    }

    /**
     * @return the onJoinResetMoney
     */
    public boolean isOnJoinResetMoney(World world) {
        return gameModes.get(world).getWorldSettings().isOnJoinResetMoney();
    }

    /**
     * @return the onJoinResetInventory
     */
    public boolean isOnJoinResetInventory(World world) {
        return gameModes.get(world).getWorldSettings().isOnJoinResetInventory();
    }

    /**
     * @return the onJoinResetEnderChest
     */
    public boolean isOnJoinResetEnderChest(World world) {
        return gameModes.get(world).getWorldSettings().isOnJoinResetEnderChest();
    }

    /**
     * @return the onLeaveResetMoney
     */
    public boolean isOnLeaveResetMoney(World world) {
        return gameModes.get(world).getWorldSettings().isOnLeaveResetMoney();
    }

    /**
     * @return the onLeaveResetInventory
     */
    public boolean isOnLeaveResetInventory(World world) {
        return gameModes.get(world).getWorldSettings().isOnLeaveResetInventory();
    }

    /**
     * @return the onLeaveResetEnderChest
     */
    public boolean isOnLeaveResetEnderChest(World world) {
        return gameModes.get(world).getWorldSettings().isOnLeaveResetEnderChest();
    }

    /**
     * Get data folder for the addon that registered this world
     *
     * @return data folder file object or the plugin's data folder if none found
     */
    public File getDataFolder(World world) {
        return getAddon(world).map(GameModeAddon::getDataFolder).orElse(plugin.getDataFolder());
    }

    /**
     * Get the game mode addon associated with this world set
     *
     * @param world
     *            - world
     * @return GameModeAddon, or empty
     */
    public Optional<GameModeAddon> getAddon(@Nullable World world) {
        return world == null ? Optional.empty() : Optional.ofNullable(gameModes.get(world));
    }

    /**
     * Get default island flag settings for this world.
     *
     * @param world
     *            - world
     * @return default rank settings for new islands.
     */
    public Map<Flag, Integer> getDefaultIslandFlags(World world) {
        return gameModes.get(world).getWorldSettings().getDefaultIslandFlags();
    }

    public List<String> getVisibleSettings(World world) {
        return gameModes.get(world).getWorldSettings().getVisibleSettings();
    }

    /**
     * Return island setting defaults for world
     *
     * @param world
     *            - world
     * @return default settings for new islands
     */
    public Map<Flag, Integer> getDefaultIslandSettings(World world) {
        return gameModes.get(world).getWorldSettings().getDefaultIslandSettings();
    }

    public boolean isUseOwnGenerator(World world) {
        return gameModes.get(world).getWorldSettings().isUseOwnGenerator();
    }

    /**
     * Return banned commands for visitors
     * @return the visitorbannedcommands
     */
    public List<String> getVisitorBannedCommands(World world) {
        return gameModes.get(world).getWorldSettings().getVisitorBannedCommands();
    }

    /**
     * Check if water is not safe, e.g., it is acid, in the world
     * @param world - world
     * @return true if water is not safe, e.g.for home locations
     */
    public boolean isWaterNotSafe(World world) {
        return gameModes.get(world).getWorldSettings().isWaterUnsafe();
    }

    /**
     * Get a list of entity types that should not exit the island limits
     * @param world - world
     * @return list
     */
    public List<String> getGeoLimitSettings(World world) {
        return gameModes.get(world).getWorldSettings().getGeoLimitSettings();
    }

    /**
     * Get the reset limit for this world
     * @param world - world
     * @return number of resets allowed. -1 = unlimited
     */
    public int getResetLimit(World world) {
        return gameModes.containsKey(world) ? gameModes.get(world).getWorldSettings().getResetLimit() : -1;
    }


    /**
     * Gets the time stamp for when all player resets were zeroed
     * @param world - world
     */
    public long getResetEpoch(World world) {
        return gameModes.get(world).getWorldSettings().getResetEpoch();
    }

    /**
     * Sets the time stamp for when all player resets were zeroed
     * @param world - world
     */
    public void setResetEpoch(World world) {
        gameModes.get(world).getWorldSettings().setResetEpoch(System.currentTimeMillis());
    }

    public boolean isTeamJoinDeathReset(World world) {
        return gameModes.get(world).getWorldSettings().isTeamJoinDeathReset();
    }

    /**
     * Get the maximum number of deaths allowed in this world
     * @param world - world
     * @return max deaths
     */
    public int getDeathsMax(World world) {
        return gameModes.get(world).getWorldSettings().getDeathsMax();
    }

    public int getBanLimit(World world) {
        return gameModes.get(world).getWorldSettings().getBanLimit();
    }
}
