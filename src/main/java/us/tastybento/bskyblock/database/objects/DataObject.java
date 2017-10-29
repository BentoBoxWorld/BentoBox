package us.tastybento.bskyblock.database.objects;

/**
 * Contains fields that must be in any data object
 * @author tastybento
 *
 */
public abstract class DataObject {
	
    /**
     * @return the uniqueId
     */
    public abstract String getUniqueId();

    /**
     * @param uniqueId the uniqueId to set
     */
    public abstract void setUniqueId(String uniqueId);
    
}
