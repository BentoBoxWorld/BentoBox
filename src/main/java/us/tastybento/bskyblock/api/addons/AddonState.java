package us.tastybento.bskyblock.api.addons;

/**
 * Represents the current run-time state of a {@link Addon}.
 *
 * @author Poslovitch
 * @since 1.0
 */
public enum AddonState {
    /**
     * The addon is being loaded. It has just been found by the {@link us.tastybento.bskyblock.managers.AddonsManager}.
     */
    LOADING,

    /**
     * The addon has been correctly loaded and is being enabled. It is currently registering its content into the different Managers.
     */
    ENABLING,

    /**
     * The addon has been correctly enabled and is now fully working.
     */
    ENABLED,

    /**
     * The addon has somehow been asked to reload and is doing so. The reload could have been ordered by an user or another addon.
     */
    RELOADING,

    /**
     * The addon is being disabled. This could have been ordered by an user or by the server shutting down.
     */
    DISABLING,

    /**
     * The addon is fully disabled.
     */
    DISABLED,

    /**
     * The addon has not been loaded because it requires a different version of BSkyBlock or of the server software.
     */
    INCOMPATIBLE,

    /**
     * The addon loading or enabling process has been interrupted by an unhandled error.
     */
    ERROR
}
