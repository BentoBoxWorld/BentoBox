package world.bentobox.bentobox.api.addons.exception;

/**
 * @deprecated Moved to {@link world.bentobox.bentobox.api.addons.exceptions.AddonException}.
 */
@Deprecated
public abstract class AddonException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 4203162022348693854L;

    public AddonException(String errorMessage){
        super("AddonException : " + errorMessage);
    }

}
