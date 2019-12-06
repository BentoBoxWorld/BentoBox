package world.bentobox.bentobox.api.addons.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.Validate;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;

/**
 * API to enable plugins to request data from addons.
 * Addons can expose data that they want to expose. To access it, call this class with the appropriate addon name, the label for the
 * data that is requested and if required, a map of key-value pairs that will be given to the addon.
 * @author HyKurtis
 *
 */
public class AddonRequestBuilder
{
    private String addonName;
    private String requestLabel;
    private Map<String, Object> metaData = new HashMap<>();

    /**
     * Define the addon you wish to request.
     *
     * @param addonName addon name
     */
    public AddonRequestBuilder addon(String addonName) {
        this.addonName = addonName;
        return this;
    }

    /**
     * Define label for addon request.
     *
     * @param requestLabel request label
     */
    public AddonRequestBuilder label(String requestLabel) {
        this.requestLabel = requestLabel;
        return this;
    }

    /**
     * Add meta data to addon request.
     *
     * @param key key
     * @param value value
     */
    public AddonRequestBuilder addMetaData(String key, Object value) {
        metaData.put(key, value);
        return this;
    }

    /**
     * Send request to addon.
     *
     * @return request response, null if no response.
     */
    public Object request() {
        Validate.notNull(addonName);
        Validate.notNull(requestLabel);

        Optional<Addon> addonOptional = BentoBox.getInstance().getAddonsManager().getAddonByName(addonName);
        if(addonOptional.isPresent()) {
            Addon addon = addonOptional.get();
            return addon.request(requestLabel, metaData);
        }
        return null;
    }
}
