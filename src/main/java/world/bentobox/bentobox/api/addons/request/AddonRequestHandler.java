package world.bentobox.bentobox.api.addons.request;

import java.util.Map;

public abstract class AddonRequestHandler
{
	private String label;

	public AddonRequestHandler(String label) {
		this.label = label.toLowerCase();
	}

	/**
	 * Get request handler label.
	 *
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Handle an addon request.
	 * This is used only for Addons to respond to addon requests from plugins.
	 * Example: request island level from Levels addon.
	 *
	 * @param metaData
	 * @return request response
	 */
	public abstract Object handle(Map<String, Object> metaData);
}
