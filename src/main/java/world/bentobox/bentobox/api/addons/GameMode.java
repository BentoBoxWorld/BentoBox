package world.bentobox.bentobox.api.addons;

import world.bentobox.bentobox.api.worlds.GameWorld;

import java.util.List;

/**
 * Specific {@link Addon} implementation providing the ability to register {@link world.bentobox.bentobox.api.worlds.GameWorld}(s).
 *
 * @author Poslovitch
 */
public abstract class GameMode extends Addon {

    public void registerGameWorld(GameWorld gameWorld) {
        //TODO
    }

    /**
     * Returns a list of the GameWorlds registered by this GameMode.
     * @return list of the GameWorlds registered by this GameMode, may be empty.
     */
    public List<GameWorld> getGameWorlds() {
        return null; //TODO
    }
}
