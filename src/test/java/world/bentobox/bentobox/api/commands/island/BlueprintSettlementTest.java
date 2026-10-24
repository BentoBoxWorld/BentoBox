package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import world.bentobox.bentobox.hooks.VaultHook.CapturedWithdrawal;

class BlueprintSettlementTest {
    @TempDir Path directory;
    private final UUID player = UUID.randomUUID();
    private final CapturedWithdrawal withdrawal = mock(CapturedWithdrawal.class);

    @BeforeEach void setup() {
        when(withdrawal.providerIdentity()).thenReturn("provider");
        when(withdrawal.get()).thenReturn(new EconomyResponse(100, 0, ResponseType.SUCCESS, null));
        when(withdrawal.refund()).thenReturn(new EconomyResponse(100, 100, ResponseType.SUCCESS, null));
    }

    private BlueprintSettlement payment() throws Exception {
        return new BlueprintSettlement(directory, player, "world", "paid", 100, withdrawal);
    }

    private Properties receipt() throws Exception {
        try (var files = Files.list(directory); var input = Files.newInputStream(files.findFirst().orElseThrow())) {
            var result = new Properties(); result.load(input); return result;
        }
    }

    @Test void writeAheadRecordExistsBeforeWithdrawalAndRefund() throws Exception {
        when(withdrawal.get()).thenAnswer(call -> {
            assertEquals("DEBIT_UNKNOWN", receipt().getProperty("state"));
            return new EconomyResponse(100, 0, ResponseType.SUCCESS, null);
        });
        when(withdrawal.refund()).thenAnswer(call -> {
            assertEquals("REFUND_UNKNOWN", receipt().getProperty("state"));
            return new EconomyResponse(100, 100, ResponseType.SUCCESS, null);
        });
        var payment = payment();
        assertTrue(payment.debit());
        assertTrue(payment.debit());
        assertTrue(payment.failed(true));
        assertTrue(payment.failed(true));
        assertEquals("REFUNDED", receipt().getProperty("state"));
        verify(withdrawal).get(); verify(withdrawal).refund();
        assertNotNull(payment());
    }

    @Test void preparedAndDebitedReceiptsBlockFreshWithdrawalsAfterRestart() throws Exception {
        var payment = payment();
        assertThrows(java.io.IOException.class, this::payment);
        payment.debit();
        assertThrows(java.io.IOException.class, this::payment);
    }

    @Test void unknownRefundIsNeverRetriedAndBlocksNewPayments() throws Exception {
        when(withdrawal.refund()).thenThrow(new IllegalStateException("unknown refund"));
        var payment = payment(); payment.debit();
        assertThrows(IllegalStateException.class, () -> payment.failed(true));
        assertFalse(payment.failed(true));
        verify(withdrawal).refund();
        assertEquals("REFUND_UNKNOWN", receipt().getProperty("state"));
        assertThrows(java.io.IOException.class, this::payment);
    }

    @Test void partialCommitPreservesIslandAndMoneyForReview() throws Exception {
        var payment = payment(); payment.debit(); payment.allocated("new-island"); payment.committing();
        assertFalse(payment.failed(false));
        verify(withdrawal, never()).refund();
        assertEquals("new-island", receipt().getProperty("island"));
        assertEquals("COMMITTING", receipt().getProperty("state"));
        assertThrows(java.io.IOException.class, this::payment);
    }

    @Test void failedJournalWriteNeverCallsVault() throws Exception {
        var payment = payment();
        Path pending;
        try (var files = Files.list(directory)) { pending = Path.of(files.findFirst().orElseThrow() + ".tmp"); }
        Files.createDirectory(pending);
        assertThrows(java.io.IOException.class, payment::debit);
        assertFalse(payment.debit());
        verify(withdrawal, never()).get();
    }
}
