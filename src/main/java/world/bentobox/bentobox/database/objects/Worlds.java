package world.bentobox.bentobox.database.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

/**
 * Tracks player worlds
 *
 * @author tastybento
 */
@Table(name = "Worlds")
public class Worlds implements DataObject {

    @Expose
    private String uniqueId;
    @Expose
    private Map<String, String> worlds = new HashMap<>();

    /**
     * @param uuid - player's UUID
     */
    public Worlds(UUID uuid) {
        uniqueId = uuid.toString();
    }

    public Worlds() {}

    /**
     * @param uuid - player's uuid
     * @param name - name of game mode
     * @param worldName - world name for this game mode
     */
    public Worlds(UUID uuid, @NonNull String name, String worldName) {
        uniqueId = uuid.toString();
        worlds.put(name, worldName);
    }

    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the worlds
     */
    public Map<String, String> getWorlds() {
        return worlds;
    }

    /**
     * @param worlds the worlds to set
     */
    public void setWorlds(Map<String, String> worlds) {
        this.worlds = worlds;
    }


}
