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
    private final Path file;
    private final Properties receipt = new Properties();
    private final CapturedWithdrawal withdrawal;
    private String state = "PREPARED";

    BlueprintSettlement(Path directory, UUID player, String world, String bundle, double amount,
            CapturedWithdrawal withdrawal) throws IOException {
        Files.createDirectories(directory);
        // shortcut: receipts are scanned per paid request; archive terminal receipts if this becomes a large ledger.
        try (var files = Files.list(directory)) {
            for (Path existing : files.filter(p -> p.toString().endsWith(".properties")).toList()) {
                Properties previous = new Properties();
                try (var input = Files.newInputStream(existing)) { previous.load(input); }
                String status = previous.getProperty("state", "UNKNOWN");
                if (player.toString().equals(previous.getProperty("player"))
                        && !java.util.Set.of("COMPLETED", "REFUNDED", "REJECTED").contains(status)) {
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

    synchronized boolean debit() throws IOException {
        if (!state.equals("PREPARED")) return state.equals("DEBITED");
        transition("DEBIT_UNKNOWN");
        var response = withdrawal.get();
        if (response == null) return false;
        receipt.setProperty("debited-world", withdrawal.worldName() == null ? "<global>" : withdrawal.worldName());
        transition(response.transactionSuccess() ? "DEBITED" : "REJECTED");
        return response.transactionSuccess();
    }

    synchronized void committing() throws IOException { transition("COMMITTING"); }
    synchronized void allocated(String island) throws IOException {
        receipt.setProperty("island", island);
        transition(state);
    }
    synchronized void complete(String island) throws IOException {
        receipt.setProperty("island", island);
        transition("COMPLETED");
    }

    synchronized boolean failed(boolean rolledBack) throws IOException {
        if (!state.equals("DEBITED") && !(rolledBack && state.equals("COMMITTING"))) return state.equals("REFUNDED");
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
                if (!java.util.Set.of("COMPLETED", "REFUNDED", "REJECTED").contains(receipt.getProperty("state", "UNKNOWN"))) {
                    plugin.logError("Unresolved blueprint payment (manual reconciliation, no automatic replay): " + file);
                }
            }
        } catch (IOException | IllegalArgumentException error) {
            plugin.logError("Could not inspect blueprint payment receipts: " + error);
        }
    }
}
