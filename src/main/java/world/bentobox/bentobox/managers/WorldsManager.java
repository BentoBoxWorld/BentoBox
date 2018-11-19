package world.bentobox.bentobox.managers;

import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameMode;
import world.bentobox.bentobox.api.worlds.GameWorld;
import world.bentobox.bentobox.hooks.MultiverseCoreHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages {@link world.bentobox.bentobox.api.worlds.GameWorld} that are registered by {@link world.bentobox.bentobox.api.addons.GameMode} addons.
 *
 * @author Poslovitch
 */
public class WorldsManager {

    private BentoBox plugin;

    private Map<GameMode, List<GameWorld>> gameWorlds;

    public WorldsManager(BentoBox plugin) {
        this.plugin = plugin;
        this.gameWorlds = new HashMap<>();
    }

    // Register
    public boolean registerGameWorld(GameMode gameMode, GameWorld gameWorld) {
        // Make sure no worlds with this name already exists
        if (getGameWorld(gameWorld.getName()).isPresent()) {
            return false;
        }

        gameWorlds.putIfAbsent(gameMode, new ArrayList<>());

        // Try to generate the worlds
        if (gameWorld.createWorlds()) {
            // Actually register the gameworld
            List<GameWorld> gameModeWorlds = gameWorlds.get(gameMode);
            gameModeWorlds.add(gameWorld);
            gameWorlds.put(gameMode, gameModeWorlds);
            return true;
        }

        // Couldn't generate the worlds
        return false;
    }

    // Getters
    public Map<GameMode, List<GameWorld>> getGameWorldsMap() {
        return gameWorlds;
    }

    public List<GameWorld> getGameWorldsList() {
        List<GameWorld> result = new ArrayList<>();
        gameWorlds.values().forEach(list -> Collections.addAll(result, list.toArray(new GameWorld[0])));
        return result;
    }

    public List<GameWorld> getGameWorlds(GameMode gameMode) {
        return gameWorlds.get(gameMode);
    }

    public Optional<GameWorld> getGameWorld(String name) {
        return getGameWorldsList().stream().filter(gameWorld -> gameWorld.getName().equals(name)).findFirst();
    }

    public Optional<GameWorld> getGameWorld(World world) {
        return getGameWorldsList().stream().filter(gameWorld -> world.equals(gameWorld.getOverWorld()) || world.equals(gameWorld.getNetherWorld()) || world.equals(gameWorld.getEndWorld())).findFirst();
    }

    // Multiverse
    public void registerWorldsToMultiverse() {
        getGameWorldsList().forEach(this::registerWorldToMultiverse);
    }

    /**
     * Registers a world with Multiverse if Multiverse is available.
     *
     * @param gameWorld the GameWorld to register
     */
    public void registerWorldToMultiverse(GameWorld gameWorld) {
        if (!gameWorld.getSettings().isUseOwnGenerator() && plugin.getHooks() != null) {
            plugin.getHooks().getHook("Multiverse-Core").ifPresent(hook -> ((MultiverseCoreHook) hook).registerWorlds(gameWorld.getOverWorld(), gameWorld.getNetherWorld(), gameWorld.getEndWorld()));
        }
    }
}
