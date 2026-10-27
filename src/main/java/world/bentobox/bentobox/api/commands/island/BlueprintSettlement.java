package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.hooks.VaultHook.CapturedWithdrawal;

/** Write-ahead receipts: Vault has no transaction IDs, so ambiguous outcomes require operator reconciliation. */
public final class BlueprintSettlement {
    /** A receipt in one of these states is finished: it is archived and never blocks a new payment. */
    private static final java.util.Set<String> TERMINAL = java.util.Set.of("COMPLETED", "REFUNDED", "REJECTED");
    private static final String ARCHIVE = "archive";

    private final Path file;
    private final Properties receipt = new Properties();
    private final CapturedWithdrawal withdrawal;
    private String state = "PREPARED";

    BlueprintSettlement(Path directory, UUID player, String world, String bundle, double amount,
            CapturedWithdrawal withdrawal) throws IOException {
        Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            for (Path existing : files.filter(p -> p.toString().endsWith(".properties")).toList()) {
                Properties previous = new Properties();
                try (var input = Files.newInputStream(existing)) { previous.load(input); }
                String status = previous.getProperty("state", "UNKNOWN");
                if (TERMINAL.contains(status) || "PREPARED".equals(status)) {
                    // Terminal, or written before debit() was reached: no Vault call can have
                    // happened, so the receipt can never represent money taken. Archive it instead
                    // of blocking this player's island creation forever.
                    archiveReceipt(existing);
                    continue;
                }
                if (player.toString().equals(previous.getProperty("player"))) {
                    throw new IOException("Unresolved blueprint payment: " + existing.getFileName());
                }
            }
        }
        this.withdrawal = withdrawal;
        file = directory.resolve(UUID.randomUUID() + ".properties");
        receipt.setProperty("player", player.toString());
        receipt.setProperty("world", world == null ? "<global>" : world);
        receipt.setProperty("bundle", bundle);
        receipt.setProperty("amount", Double.toString(amount));
        receipt.setProperty("provider", withdrawal.providerIdentity());
        transition("PREPARED");
    }

    enum DebitResult { DEBITED, REJECTED, UNKNOWN }

    synchronized DebitResult debit() throws IOException {
        if (!state.equals("PREPARED")) return state.equals("DEBITED") ? DebitResult.DEBITED : DebitResult.UNKNOWN;
        transition("DEBIT_UNKNOWN");
        var response = withdrawal.get();
        if (response == null) return DebitResult.UNKNOWN;
        receipt.setProperty("debited-world", withdrawal.worldName() == null ? "<global>" : withdrawal.worldName());
        boolean debited = response.transactionSuccess();
        transition(debited ? "DEBITED" : "REJECTED");
        return debited ? DebitResult.DEBITED : DebitResult.REJECTED;
    }

    synchronized void committing() throws IOException { transition("COMMITTING"); }
    synchronized void allocated(String island) throws IOException {
        receipt.setProperty("island", island);
        transition(state);
    }
    synchronized void complete(String island) throws IOException {
        receipt.setProperty("island", island);
        try {
            transition("COMPLETED");
        } catch (IOException error) {
            // The island is delivered and the money is spent. Keep an audit trail: append to
            // payments.log, and only drop the receipt when that worked, otherwise the last
            // written state stays on disk for an admin to reconcile.
            boolean audited = false;
            try (var log = java.nio.file.Files.newBufferedWriter(file.resolveSibling("payments.log"),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                log.write("COMPLETED_UNRECORDED " + java.time.Instant.now() + " " + file.getFileName()
                        + " player=" + receipt.getProperty("player") + " amount=" + receipt.getProperty("amount")
                        + " provider=" + receipt.getProperty("provider") + System.lineSeparator());
                audited = true;
            } catch (IOException suppressed) {
                error.addSuppressed(suppressed);
            }
            if (audited) {
                try { java.nio.file.Files.deleteIfExists(file); } catch (IOException suppressed) { error.addSuppressed(suppressed); }
            }
            throw error;
        }
    }

    synchronized boolean failed(boolean rolledBack) throws IOException {
        if (!state.equals("DEBITED") && !(rolledBack && state.equals("COMMITTING"))) {
            // Money may be gone but the island was delivered: make it obvious for operations
            // instead of leaving the receipt stuck on COMMITTING forever.
            if (state.equals("COMMITTING")) transition("REVIEW");
            return state.equals("REFUNDED");
        }
        if (!rolledBack) { transition("REVIEW"); return false; }
        transition("REFUND_UNKNOWN");
        var response = withdrawal.refund();
        if (response != null && response.transactionSuccess()) transition("REFUNDED");
        return state.equals("REFUNDED");
    }

    private void transition(String next) throws IOException {
        // Advance memory first too: an IO error must never authorize another external money operation.
        state = next;
        receipt.setProperty("state", next);
        StringWriter text = new StringWriter();
        receipt.store(text, "Blueprint payment; never automatically retry an UNKNOWN or unfinished receipt");
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(text.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        if (TERMINAL.contains(next)) {
            archiveReceipt(file);
        }
    }

    /**
     * Moves a finished receipt into {@code archive/} so the directory scanned before each
     * payment only ever holds unresolved receipts. Best effort: a failed move leaves the
     * receipt in place, which is harmless (it is still terminal).
     */
    private static void archiveReceipt(Path receiptPath) {
        Path archive = receiptPath.getParent().resolve(ARCHIVE);
        try {
            Files.createDirectories(archive);
            Files.move(receiptPath, archive.resolve(receiptPath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException error) {
            BentoBox plugin = BentoBox.getInstance();
            if (plugin != null) {
                plugin.logError("Could not archive blueprint payment receipt " + receiptPath + ": "
                        + error.getMessage());
            }
        }
    }

    static Path directory(BentoBox plugin) {
        return plugin.getDataFolder().toPath().resolve("blueprint-payments");
    }

    /** Optional startup diagnostic only: Vault cannot safely replay transactions after a crash. */
    public static void reportPending(BentoBox plugin) {
        Path directory = directory(plugin);
        if (!Files.exists(directory)) return;
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".properties")).toList()) {
                var receipt = new Properties();
                try (var input = Files.newInputStream(file)) { receipt.load(input); }
                if (TERMINAL.contains(receipt.getProperty("state", "UNKNOWN"))
                        || "PREPARED".equals(receipt.getProperty("state", "UNKNOWN"))) {
                    // Clean up receipts left behind by older versions. PREPARED means debit() was
                    // never reached, so there is no money to reconcile.
                    archiveReceipt(file);
                } else {
                    plugin.logError("Unresolved blueprint payment (manual reconciliation, no automatic replay): " + file);
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            plugin.logError("Could not inspect blueprint payment receipts: " + error);
        }
    }
}
