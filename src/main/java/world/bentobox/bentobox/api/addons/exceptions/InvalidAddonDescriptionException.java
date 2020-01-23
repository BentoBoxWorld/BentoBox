package world.bentobox.bentobox.api.addons.exceptions;

/**
 * @since 1.11.0
 */
public class InvalidAddonDescriptionException extends AddonException {

    /**
     *
     */
    private static final long serialVersionUID = 7741502900847049986L;

    public InvalidAddonDescriptionException(String errorMessage) {
        super(errorMessage);
    }
}
