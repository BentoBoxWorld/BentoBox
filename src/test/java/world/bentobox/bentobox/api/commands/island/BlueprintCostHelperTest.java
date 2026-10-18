package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.hooks.VaultHook;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.TaskScheduler;

class BlueprintCostHelperTest {
    private final BentoBox plugin = mock(BentoBox.class);
    private final GameModeAddon addon = mock(GameModeAddon.class);
    private final User user = mock(User.class);
    private final VaultHook vault = mock(VaultHook.class);

    @BeforeEach
    void setUp() {
        var blueprints = mock(BlueprintsManager.class);
        var settings = mock(Settings.class);
        var bundle = new BlueprintBundle();
        bundle.setCost(100);
        when(plugin.getBlueprintsManager()).thenReturn(blueprints);
        when(blueprints.getBlueprintBundles(addon)).thenReturn(Map.of("paid", bundle, "default", new BlueprintBundle()));
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.isUseEconomy()).thenReturn(true);
        when(plugin.getVault()).thenReturn(Optional.of(vault));
    }

    @Test
    void capturedPaymentUsesOriginalPriceAndProviderOnlyOnce() {
        var world = mock(org.bukkit.World.class);
        java.util.function.Supplier<EconomyResponse> withdrawal = mock(java.util.function.Supplier.class);
        when(vault.captureWithdrawal(user, 100, world)).thenReturn(withdrawal);
        when(withdrawal.get()).thenReturn(new EconomyResponse(100, 0, ResponseType.SUCCESS, null));
        var charge = BlueprintCostHelper.captureCharge(plugin, addon, user, "paid", world);
        plugin.getBlueprintsManager().getBlueprintBundles(addon).get("paid").setCost(900);
        when(plugin.getVault()).thenReturn(Optional.empty());
        when(user.getWorld()).thenThrow(new IllegalStateException("disconnected"));
        assertTrue(charge.getAsBoolean());
        assertTrue(charge.getAsBoolean());
        verify(withdrawal).get();
        verify(vault).captureWithdrawal(user, 100, world);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void unknownCapturedPaymentIsNeverRetried(boolean throwsAfterWithdrawal) {
        var world = mock(org.bukkit.World.class);
        java.util.function.Supplier<EconomyResponse> withdrawal = mock(java.util.function.Supplier.class);
        when(vault.captureWithdrawal(user, 100, world)).thenReturn(withdrawal);
        if (throwsAfterWithdrawal) when(withdrawal.get()).thenThrow(new IllegalStateException("outcome unknown"));
        var charge = BlueprintCostHelper.captureCharge(plugin, addon, user, "paid", world);
        try (var tasks = org.mockito.Mockito.mockStatic(TaskScheduler.class)) {
            assertFalse(charge.getAsBoolean());
            assertFalse(charge.getAsBoolean());
            verify(withdrawal).get();
            verify(plugin).logError(anyString());
        }
    }

    @Test
    void providerWithdrawalDecidesPaymentWithoutSeparateBalanceCheck() {
        when(vault.withdraw(user, 100)).thenReturn(new EconomyResponse(100, 0, ResponseType.SUCCESS, null));
        assertTrue(BlueprintCostHelper.checkCost(plugin, addon, user, "paid", true));
        verify(vault, never()).has(user, 100);
        verify(vault).withdraw(user, 100);
    }

    @Test
    void missingWithdrawalResponseIsReportedAsFailure() {
        assertFalse(BlueprintCostHelper.checkCost(plugin, addon, user, "paid", true));
        verify(plugin).logError(anyString());
        verify(user).sendMessage("general.errors.general");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = ResponseType.class, names = {"FAILURE", "NOT_IMPLEMENTED"})
    void unsuccessfulProviderResponseNeverCountsAsPayment(ResponseType type) {
        when(vault.withdraw(user, 100)).thenReturn(new EconomyResponse(0, 100, type, "provider rejected payment"));
        assertFalse(BlueprintCostHelper.checkCost(plugin, addon, user, "paid", true));
        verify(plugin).logError(anyString());
        verify(user).sendMessage("general.errors.general");
    }

    @Test
    void affordabilityCheckDoesNotWithdraw() {
        when(vault.has(user, 100)).thenReturn(true);
        assertTrue(BlueprintCostHelper.checkCost(plugin, addon, user, "paid", false));
        verify(vault, never()).withdraw(user, 100);
    }
}
