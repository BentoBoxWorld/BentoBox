/**
 * 
 */
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
    abstract public String getUniqueId();

    /**
     * @param uniqueId the uniqueId to set
     */
    abstract public void setUniqueId(String uniqueId);
    

}
