package world.bentobox.bentobox.api.addons;

/**
 * Represents the current run-time state of a {@link Addon}.
 *
 * @author Poslovitch
 * @since 1.0
 */
public enum AddonState {
    /**
     * The addon has been correctly enabled and is now fully working.
     */
    ENABLED,

    /**
     * The addon is fully disabled.
     */
    DISABLED,

    /**
     * The addon has not been loaded because it requires a different version of BentoBox or of the server software.
     */
    INCOMPATIBLE,

    /**
     * The addon has not been enabled because a dependency is missing.
     */
    MISSING_DEPENDENCY,

    /**
     * The addon loading or enabling process has been interrupted by an unhandled error.
     */
    ERROR
}
