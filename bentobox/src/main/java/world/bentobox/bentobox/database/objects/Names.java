package world.bentobox.bentobox.database.objects;

import java.util.UUID;

import com.google.gson.annotations.Expose;

/**
 * Stores player names and uuid's
 * @author tastybento
 *
 */
public class Names implements DataObject {

    @Expose
    private String uniqueId = ""; // name
    @Expose
    private UUID uuid;
    
    public Names() {}
    
    public Names(String name, UUID uuid) {
        this.uniqueId = name;
        this.uuid = uuid;
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
     * @return the uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }


}
