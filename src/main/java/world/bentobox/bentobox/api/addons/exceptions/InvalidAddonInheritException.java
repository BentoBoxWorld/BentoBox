package world.bentobox.bentobox.api.addons.exceptions;

import java.io.Serial;

public class InvalidAddonInheritException extends AddonException {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5847358994397613244L;

    public InvalidAddonInheritException(String errorMessage) {
        super(errorMessage);
    }

}
