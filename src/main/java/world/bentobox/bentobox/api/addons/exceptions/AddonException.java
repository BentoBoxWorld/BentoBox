package world.bentobox.bentobox.api.addons.exceptions;

import java.io.Serial;

/**
 * A base class for exceptions related to BentoBox addons.
 * <p>
 * This abstract class serves as the superclass for more specific addon-related
 * exceptions, allowing for centralized handling of addon loading and runtime errors.
 *
 * @author Poslovitch
 * @since 1.0
 */
public abstract class AddonException extends Exception {

    /**
     * The serial version UID for serialization.
     */
    @Serial
    private static final long serialVersionUID = 4203162022348693854L;

    /**
     * Constructs a new AddonException with the specified error message.
     *
     * @param errorMessage The detail message.
     */
    protected AddonException(String errorMessage){
        super("AddonException : " + errorMessage);
    }

}
