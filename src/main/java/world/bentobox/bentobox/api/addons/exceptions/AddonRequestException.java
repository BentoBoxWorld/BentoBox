package world.bentobox.bentobox.api.addons.exceptions;

import java.io.Serial;

public class AddonRequestException extends AddonException
{
	@Serial
    private static final long serialVersionUID = -5698456013070166174L;

	public AddonRequestException(String errorMessage) {
		super(errorMessage);
	}
}
