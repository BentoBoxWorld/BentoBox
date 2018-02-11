package us.tastybento.bskyblock.database.objects;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Contains fields that must be in any data object
 * @author tastybento
 *
 */
public interface DataObject {

    default BSkyBlock getPlugin() {
        return BSkyBlock.getInstance();
    }

    /**
     * @return the uniqueId
     */
    String getUniqueId();

    /**
     * @param uniqueId the uniqueId to set
     */
    void setUniqueId(String uniqueId);

}
