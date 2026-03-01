package world.bentobox.bentobox.api.commands.island;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Shared cost-checking logic for island create and reset commands.
 * Checks if the user can afford a blueprint bundle cost, and optionally charges them.
 * Cost is only applied when multiple bundles are available and economy is enabled.
 */
class BlueprintCostHelper {

    private BlueprintCostHelper() {}

    /**
     * Checks if the user can afford the blueprint bundle cost, and optionally charges them.
     *
     * @param plugin The BentoBox plugin instance
     * @param addon The game mode addon
     * @param user The user to check/charge
     * @param name The blueprint bundle name
     * @param charge If true, withdraw the cost; if false, just check affordability
     * @return true if cost check passes (affordable or not applicable), false if cannot afford
     */
    static boolean checkCost(BentoBox plugin, GameModeAddon addon, User user, String name, boolean charge) {
        if (plugin.getBlueprintsManager().getBlueprintBundles(addon).size() <= 1) {
            return true; // Cost ignored for single bundle
        }
        if (!plugin.getSettings().isUseEconomy()) {
            return true;
        }
        BlueprintBundle bundle = plugin.getBlueprintsManager()
                .getBlueprintBundles(addon).get(name);
        if (bundle == null || bundle.getCost() <= 0) {
            return true;
        }
        return plugin.getVault().map(vault -> {
            if (!vault.has(user, bundle.getCost())) {
                user.sendMessage("commands.island.create.cannot-afford",
                        TextVariables.COST, vault.format(bundle.getCost()));
                return false;
            }
            if (charge) {
                vault.withdraw(user, bundle.getCost());
            }
            return true;
        }).orElse(true); // No vault = skip silently
    }
}
