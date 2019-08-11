package world.bentobox.bentobox.api.flags;

/**
 * Defines the flag mode
 * @author tastybento
 * @since 1.6.0
 *
 */
public enum FlagMode {
    /**
     * Flag should be shown in the basic settings
     */
    BASIC,
    /**
     * Flag should be shown in the advanced settings
     */
    ADVANCED,
    /**
     * Flag should be shown in the expert settings
     */
    EXPERT,
    /**
     * Flag should be shown in the top row if applicable
     */
    TOP_ROW;

    /**
     * Get the next ranking flag above this one. If at the top, it cycles back to the bottom rank
     * @return next rank
     */
    public FlagMode getNextFlag() {
        switch(this) {
        case ADVANCED:
            return FlagMode.EXPERT;
        case BASIC:
            return FlagMode.ADVANCED;
        default:
            return FlagMode.BASIC;
        }
    }

    /**
     * Get a list of ranks that are ranked greater than this rank
     * @param rank - rank to compare
     * @return true if ranked greater
     */
    public boolean isGreaterThan(FlagMode rank) {
        switch(this) {
        case EXPERT:
            return rank.equals(BASIC) || rank.equals(ADVANCED);
        case ADVANCED:
            return rank.equals(BASIC);
        default:
            return false;
        }
    }

}
