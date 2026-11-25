package world.bentobox.bentobox.blueprints;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Says whether a player can use a blueprint bundle right now. Game modes decide this in
 * {@link world.bentobox.bentobox.api.addons.GameModeAddon#getBundleAvailability}.
 *
 * @param state whether the bundle can be used, is shown but cannot be used, or is not shown
 * @param message for a locked bundle, the reason, already translated for the player. It is
 *                shown on the bundle in the selection panel and sent to the player if they
 *                ask for the bundle by name.
 * @author tastybento
 * @since 3.25.0
 */
public record BundleAvailability(@NonNull State state, @Nullable String message) {

    /**
     * Whether a bundle can be used
     */
    public enum State {
        /**
         * The player can use the bundle
         */
        AVAILABLE,
        /**
         * The player can see the bundle but cannot use it yet
         */
        LOCKED,
        /**
         * The player cannot see or use the bundle
         */
        HIDDEN
    }

    private static final BundleAvailability IS_AVAILABLE = new BundleAvailability(State.AVAILABLE, null);
    private static final BundleAvailability IS_HIDDEN = new BundleAvailability(State.HIDDEN, null);

    /**
     * @return the player can use the bundle
     */
    public static BundleAvailability available() {
        return IS_AVAILABLE;
    }

    /**
     * @param message the reason, already translated for the player
     * @return the player can see the bundle but cannot use it yet
     */
    public static BundleAvailability locked(@NonNull String message) {
        return new BundleAvailability(State.LOCKED, message);
    }

    /**
     * @return the player cannot see or use the bundle
     */
    public static BundleAvailability hidden() {
        return IS_HIDDEN;
    }

    /**
     * @return true if the player can use the bundle
     */
    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }
}
