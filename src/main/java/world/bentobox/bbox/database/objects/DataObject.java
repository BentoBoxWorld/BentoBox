package world.bentobox.bbox.database.objects;

import world.bentobox.bbox.BentoBox;

/**
 * Contains fields that must be in any data object
 * @author tastybento
 *
 */
public interface DataObject {

    default BentoBox getPlugin() {
        return BentoBox.getInstance();
    }

    /**
     * @return the uniqueId
     */
    String getUniqueId();

    /**
     * @param uniqueId - unique ID the uniqueId to set
     */
    void setUniqueId(String uniqueId);

}
