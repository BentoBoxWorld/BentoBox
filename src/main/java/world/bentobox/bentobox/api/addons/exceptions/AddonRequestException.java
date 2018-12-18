package world.bentobox.bentobox.api.addons.exceptions;

import world.bentobox.bentobox.api.addons.request.AddonRequestBuilder;

import java.util.UUID;

public class AddonRequestException extends AddonException
{
	private static final long serialVersionUID = -5698456013070166174L;

	public AddonRequestException(String errorMessage) {
		super(errorMessage);
	}
}
