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
        Path file;
        try (var files = Files.list(directory)) {
            file = files.filter(p -> p.toString().endsWith(".properties")).findFirst().orElse(null);
        }
        if (file == null) {
            try (var files = Files.list(directory.resolve("archive"))) {
                file = files.findFirst().orElseThrow();
            }
        }
        try (var input = Files.newInputStream(file)) {
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
        assertEquals(BlueprintSettlement.DebitResult.DEBITED, payment.debit());
        assertEquals(BlueprintSettlement.DebitResult.DEBITED, payment.debit());
        assertTrue(payment.failed(true));
        assertTrue(payment.failed(true));
        assertEquals("REFUNDED", receipt().getProperty("state"));
        verify(withdrawal).get(); verify(withdrawal).refund();
        assertNotNull(payment());
    }

    @Test void completedReceiptIsArchived() throws Exception {
        var payment = payment();
        payment.debit();
        payment.committing();
        payment.complete("new-island");
        // Nothing terminal is left in the scanned directory, so the ledger stops growing.
        try (var files = Files.list(directory)) {
            assertEquals(0, files.filter(p -> p.toString().endsWith(".properties")).count());
        }
        try (var files = Files.list(directory.resolve("archive"))) {
            assertEquals(1, files.count());
        }
        assertEquals("COMPLETED", receipt().getProperty("state"));
        assertNotNull(payment());
    }

    @Test void preparedAndDebitedReceiptsBlockFreshWithdrawalsAfterRestart() throws Exception {
        var payment = payment();
        // PREPARED means debit() was never reached: no money can have moved, so it is archived
        // rather than blocking this player's next island creation.
        assertNotNull(payment());
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
        assertEquals("REVIEW", receipt().getProperty("state"));
        assertThrows(java.io.IOException.class, this::payment);
    }

    @Test void failedJournalWriteNeverCallsVault() throws Exception {
        var payment = payment();
        Path pending;
        try (var files = Files.list(directory)) { pending = Path.of(files.findFirst().orElseThrow() + ".tmp"); }
        Files.createDirectory(pending);
        assertThrows(java.io.IOException.class, payment::debit);
        assertEquals(BlueprintSettlement.DebitResult.UNKNOWN, payment.debit());
        verify(withdrawal, never()).get();
    }
}
