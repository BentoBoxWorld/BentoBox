package world.bentobox.bentobox.database.objects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * World-level settings that BentoBox stores itself, one record per game mode, keyed by the
 * game mode addon's name. Unlike {@link world.bentobox.bentobox.api.configuration.WorldSettings},
 * which each game mode implements and saves in its own config.yml, these settings need no
 * game mode support: BentoBox loads, seeds and saves them through its database.
 * <p>
 * Read and edit them via {@link world.bentobox.bentobox.managers.IslandWorldManager}.
 * @author tastybento
 * @since 3.23.6
 */
@Table(name = "WorldSettings")
public class CoreWorldSettings implements DataObject {

    @Expose
    private String uniqueId = ""; // game mode addon name

    /**
     * Entity types that may always spawn, regardless of the animal and monster spawning flags
     */
    @Expose
    private List<String> allowedMobs = new ArrayList<>();

    public CoreWorldSettings() {
        // Required for deserialization
    }

    /**
     * @param gameModeName - name of the game mode addon these settings belong to
     */
    public CoreWorldSettings(String gameModeName) {
        this.uniqueId = gameModeName;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return mutable list of entity type names that may always spawn
     */
    public List<String> getAllowedMobs() {
        if (allowedMobs == null) {
            allowedMobs = new ArrayList<>();
        }
        return allowedMobs;
    }

    /**
     * @param allowedMobs the entity type names that may always spawn
     */
    public void setAllowedMobs(List<String> allowedMobs) {
        this.allowedMobs = allowedMobs;
    }
}
