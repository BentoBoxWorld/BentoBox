package world.bentobox.bentobox.api.configuration;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 * Config object for YAML objects
 * @author tastybento
 * @since 1.5.0
 */
public interface ConfigObject extends DataObject {

    @Override
    default BentoBox getPlugin() {
        return BentoBox.getInstance();
    }

    /**
     * @return the uniqueId
     */
    @Override
    default String getUniqueId() {
        return "config";
    }

    /**
     * @param uniqueId - unique ID the uniqueId to set
     */
    @Override
    default void setUniqueId(String uniqueId) {}
}
