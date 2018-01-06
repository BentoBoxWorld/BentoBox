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
    abstract String getUniqueId();

    /**
     * @param uniqueId the uniqueId to set
     */
    abstract void setUniqueId(String uniqueId);
    
}
