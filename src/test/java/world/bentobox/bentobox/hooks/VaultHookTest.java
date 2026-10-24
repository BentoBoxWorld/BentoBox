package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.api.user.User;

class VaultHookTest {
    private final Economy economy = mock(Economy.class);
    private final User user = mock(User.class);
    private final OfflinePlayer account = mock(OfflinePlayer.class);
    private final World world = mock(World.class);
    private VaultHook hook;

    @BeforeEach
    void setUp() throws Exception {
        hook = new VaultHook();
        var field = VaultHook.class.getDeclaredField("economy");
        field.setAccessible(true);
        field.set(hook, economy);
        when(user.isOfflinePlayer()).thenReturn(true);
        when(user.getOfflinePlayer()).thenReturn(account);
        when(user.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("islands");
    }

    @Test
    void refundableFallbackCreditsOnlyTheAccountActuallyDebitedOnce() {
        var unsupported = new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "no worlds");
        var success = new EconomyResponse(100, 0, ResponseType.SUCCESS, null);
        when(economy.withdrawPlayer(account, "islands", 100)).thenReturn(unsupported);
        when(economy.withdrawPlayer(account, 100)).thenReturn(success);
        when(economy.depositPlayer(account, 100)).thenReturn(success);
        var payment = hook.captureRefundableWithdrawal(user, 100, world);
        assertSame(success, payment.get());
        assertSame(success, payment.get());
        assertSame(success, payment.refund());
        assertSame(success, payment.refund());
        verify(economy).withdrawPlayer(account, 100);
        verify(economy).depositPlayer(account, 100);
        verify(economy, never()).depositPlayer(account, "islands", 100);
    }

    @Test
    void refundableUnknownDebitNeverRetriesOrRefunds() {
        when(economy.withdrawPlayer(account, "islands", 100)).thenThrow(new IllegalStateException("unknown"));
        var payment = hook.captureRefundableWithdrawal(user, 100, world);
        assertThrows(java.util.concurrent.CompletionException.class, payment::get);
        assertThrows(java.util.concurrent.CompletionException.class, payment::get);
        assertThrows(java.util.concurrent.CompletionException.class, payment::refund);
        verify(economy).withdrawPlayer(account, "islands", 100);
        verify(economy, never()).depositPlayer(account, "islands", 100);
    }

    @Test
    void delayedWithdrawalKeepsOriginalAccountProviderAndWorld() throws Exception {
        var response = new EconomyResponse(100, 0, ResponseType.SUCCESS, null);
        when(economy.withdrawPlayer(account, "islands", 100)).thenReturn(response);
        var withdrawal = hook.captureWithdrawal(user, 100, world);
        verifyNoInteractions(economy);
        when(user.getOfflinePlayer()).thenReturn(mock(OfflinePlayer.class));
        when(user.getWorld()).thenThrow(new IllegalStateException("disconnected"));
        when(world.getName()).thenReturn("other-world");
        var replacement = mock(Economy.class);
        var field = VaultHook.class.getDeclaredField("economy");
        field.setAccessible(true);
        field.set(hook, replacement);
        assertSame(response, withdrawal.get());
        verify(economy).withdrawPlayer(account, "islands", 100);
        verifyNoInteractions(replacement);
    }

    @Test
    void unknownWorldWithdrawalNeverRetriesGeneralAccount() {
        assertNull(hook.withdraw(user, 100, world));
        verify(economy).withdrawPlayer(account, "islands", 100);
        verify(economy, never()).withdrawPlayer(account, 100);
    }

    @Test
    void unknownWorldDepositNeverRetriesGeneralAccount() {
        assertNull(hook.deposit(user, 100, world));
        verify(economy).depositPlayer(account, "islands", 100);
        verify(economy, never()).depositPlayer(account, 100);
    }

    @Test
    void explicitlyUnsupportedWorldOperationsFallBackOnce() {
        var unsupported = new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "worlds not supported");
        var success = new EconomyResponse(100, 0, ResponseType.SUCCESS, null);
        when(economy.withdrawPlayer(account, "islands", 100)).thenReturn(unsupported);
        when(economy.withdrawPlayer(account, 100)).thenReturn(success);
        when(economy.depositPlayer(account, "islands", 100)).thenReturn(unsupported);
        when(economy.depositPlayer(account, 100)).thenReturn(success);
        assertSame(success, hook.withdraw(user, 100, world));
        assertSame(success, hook.deposit(user, 100, world));
        verify(economy).withdrawPlayer(account, 100);
        verify(economy).depositPlayer(account, 100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUCCESS", "FAILURE"})
    void definitiveWorldResponsesArePreserved(String type) {
        var response = new EconomyResponse(100, 0, ResponseType.valueOf(type), "result");
        when(economy.withdrawPlayer(account, "islands", 100)).thenReturn(response);
        when(economy.depositPlayer(account, "islands", 100)).thenReturn(response);
        assertSame(response, hook.withdraw(user, 100, world));
        assertSame(response, hook.deposit(user, 100, world));
        verify(economy, never()).withdrawPlayer(account, 100);
        verify(economy, never()).depositPlayer(account, 100);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidAmountsNeverReachProvider(double amount) {
        assertThrows(IllegalArgumentException.class, () -> hook.withdraw(user, amount));
        assertThrows(IllegalArgumentException.class, () -> hook.withdraw(user, amount, world));
        assertThrows(IllegalArgumentException.class, () -> hook.deposit(user, amount));
        assertThrows(IllegalArgumentException.class, () -> hook.deposit(user, amount, world));
        assertThrows(IllegalArgumentException.class, () -> hook.has(user, amount));
        assertThrows(IllegalArgumentException.class, () -> hook.has(user, amount, world));
        verifyNoInteractions(economy);
    }
}
