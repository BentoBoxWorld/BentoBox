/**
 * 
 */
package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.User;

/**
 * A bean to hold name to UUID lookup
 * @author tastybento
 *
 */
public class NameToUUID implements DataObject {
    
    public HashMap<String, UUID> namesToUUID;
    
    public NameToUUID() {}

    /**
     * @return the namesToUUID
     */
    public HashMap<String, UUID> getNamesToUUID() {
        return namesToUUID;
    }

    /**
     * @param namesToUUID the namesToUUID to set
     */
    public void setNamesToUUID(HashMap<String, UUID> namesToUUID) {
        this.namesToUUID = namesToUUID;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.objects.DataObject#getUniqueId()
     */
    @Override
    public String getUniqueId() {
        return "names-uuid";
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.objects.DataObject#setUniqueId(java.lang.String)
     */
    @Override
    public void setUniqueId(String uniqueId) {
        // Do nothing
    }

    /**
     * Add or update a name
     * @param user
     */
    public void addName(User user) {
        this.namesToUUID.put(user.getName(), user.getUniqueId());
    }
    
    /**
     * Get UUID for name
     * @param name
     * @return UUID or null if not found
     */
    public UUID getUUID(String name) {
        return this.namesToUUID.get(name);
    }
}
