package us.tastybento.bskyblock.api.commands;

/**
 * Response class for the canUse check
 * @author tastybento
 *
 */
public class CanUseResp {
    private boolean allowed;
    String errorResponse; // May be shown if required

    /**
     * Cannot use situation
     * @param errorResponse - error response
     */
    public CanUseResp(String errorResponse) {
        this.allowed = false;
        this.errorResponse = errorResponse;
    }

    /**
     * Can or cannot use situation, no error response.
     * @param b
     */
    public CanUseResp(boolean b) {
        this.allowed = b;
        this.errorResponse = "";
    }
    /**
     * @return the allowed
     */
    public boolean isAllowed() {
        return allowed;
    }
    /**
     * @param allowed the allowed to set
     */
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
    /**
     * @return the errorResponse
     */
    public String getErrorResponse() {
        return errorResponse;
    }
    /**
     * @param errorResponse the errorResponse to set
     */
    public void setErrorResponse(String errorResponse) {
        this.errorResponse = errorResponse;
    }
}
