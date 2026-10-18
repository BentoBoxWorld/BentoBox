package world.bentobox.bentobox.api.commands.island;

import java.util.function.BooleanSupplier;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.World;

/**
 * Shared cost-checking logic for island create and reset commands.
 * Checks if the user can afford a blueprint bundle cost, and optionally charges them.
 * Cost is only applied when multiple bundles are available and economy is enabled.
 */
class BlueprintCostHelper {

    private BlueprintCostHelper() {}

    /** Capture on the player's thread: completion may run after teleport or disconnect. */
    static BooleanSupplier captureCharge(BentoBox plugin, GameModeAddon addon, User user, String name, World world) {
        var bundles = plugin.getBlueprintsManager().getBlueprintBundles(addon);
        BlueprintBundle bundle = bundles.get(name);
        if (bundles.size() <= 1 || !plugin.getSettings().isUseEconomy()
                || bundle == null || bundle.getCost() <= 0) return () -> true;
        var provider = plugin.getVault();
        if (provider.isEmpty()) return () -> true;
        var vault = provider.get();
        double amount = bundle.getCost();
        var withdrawal = vault.captureWithdrawal(user, amount, world);
        var playerId = user.getUniqueId();
        return new BooleanSupplier() {
            private boolean attempted;
            private boolean paid;

            @Override
            public synchronized boolean getAsBoolean() {
                if (attempted) return paid;
                // A provider can throw after changing the balance: never retry an unknown outcome.
                attempted = true;
                try {
                    var response = withdrawal.get();
                    paid = response != null && response.transactionSuccess();
                    if (paid) return true;
                    plugin.logError("Blueprint payment failed for " + playerId + ": "
                            + (response == null ? "economy provider returned no response" : response.errorMessage));
                } catch (RuntimeException error) {
                    plugin.logError("Blueprint payment outcome unknown for " + playerId + "; payment will not be retried.");
                    plugin.logStacktrace(error);
                }
                TaskScheduler.user(plugin, user, () -> user.sendMessage("general.errors.general"));
                return false;
            }
        };
    }

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
            if (charge) {
                var response = vault.withdraw(user, bundle.getCost());
                if (response != null && response.transactionSuccess()) return true;
                plugin.logError("Blueprint payment failed for " + user.getUniqueId() + ": "
                        + (response == null ? "economy provider returned no response" : response.errorMessage));
                user.sendMessage("general.errors.general");
                return false;
            }
            if (!vault.has(user, bundle.getCost())) {
                user.sendMessage("commands.island.create.cannot-afford",
                        TextVariables.COST, vault.format(bundle.getCost()));
                return false;
            }
            return true;
        }).orElse(true); // No vault = skip silently
    }
}
