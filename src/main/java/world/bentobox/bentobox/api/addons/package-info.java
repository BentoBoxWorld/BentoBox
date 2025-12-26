/**
 * This package contains classes and interfaces related to BentoBox addons.
 * <p>
 * Addons are modular extensions that enhance BentoBox functionality. Game-specific
 * addons (e.g., BSkyBlock, AcidIsland) as well as generic addons (e.g., Challenges, Warps)
 * are supported by this system. Developers can create custom addons to introduce
 * new features or gamemodes.
 * <p>
 * Since BentoBox was created, server tech has changed and code remapping is done and that
 * is usually only applied when a Plugin is loaded, so developers should use Pladdons
 * which are a wrapper for Addons in a Plugin.
 * <p>
 * Key components:
 * - AddonLoader: Manages the lifecycle of addons.
 * - AddonConfig: Handles addon-specific configurations.
 *
 * @since 1.0
 * @author tastybento
 */
package world.bentobox.bentobox.api.addons;