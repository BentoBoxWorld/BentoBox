package world.bentobox.bentobox.api.addons.exceptions;

import java.io.Serial;

public abstract class AddonException extends Exception {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4203162022348693854L;

    protected AddonException(String errorMessage){
        super("AddonException : " + errorMessage);
    }

}
