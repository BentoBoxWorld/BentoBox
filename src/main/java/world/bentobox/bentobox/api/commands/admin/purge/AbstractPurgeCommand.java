package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import world.bentobox.bentobox.util.TaskScheduler;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;

/**
 * Shared scaffolding for purge subcommands that scan for region files and
 * (on confirmation) reap them via {@link world.bentobox.bentobox.managers.PurgeRegionsService}.
 *
 * <p>Subclasses supply the scan source (age-filtered or deletable-only),
 * confirmation prompt, success message and any pre-delete side-effects via
 * the abstract / overridable hooks.
 */
abstract class AbstractPurgeCommand extends CompositeCommand {

    protected static final String NONE_FOUND = "commands.admin.purge.none-found";

    /** Console has no UUID, so it gets its own fixed key. */
    private static final UUID CONSOLE_KEY = new UUID(0L, 0L);

    protected volatile boolean inPurge;
    /** A scan older than this must be re-run: the world may have changed since it was shown. */
    private static final long SCAN_EXPIRY_MS = java.time.Duration.ofMinutes(10).toMillis();

    private record PendingScan(PurgeScanResult scan, long createdAt) {
    }

    /**
     * Scan results awaiting confirmation, keyed by the admin that ran the scan.
     * Command instances are shared by every admin on the server, so a single
     * field would let one admin confirm (and delete) another admin's scan.
     */
    private final Map<UUID, PendingScan> pendingScans = new ConcurrentHashMap<>();

    protected AbstractPurgeCommand(CompositeCommand parent, String label) {
        super(parent, label);
    }

    private static UUID keyOf(User user) {
        UUID id = user.getUniqueId();
        return id == null ? CONSOLE_KEY : id;
    }

    @Override
    public boolean canExecute(User user, String label, java.util.List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        return true;
    }

    /** @return true if this user has an unexpired scan waiting for confirmation */
    protected final boolean hasPendingScan(User user) {
        PendingScan pending = pendingScans.get(keyOf(user));
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() - pending.createdAt() > SCAN_EXPIRY_MS) {
            pendingScans.remove(keyOf(user), pending);
            return false;
        }
        return true;
    }

    /** Drops any scan this user has not confirmed yet. */
    protected final void clearPendingScan(User user) {
        pendingScans.remove(keyOf(user));
    }

    /** @return the unexpired scan for this user, removing it so it can only be confirmed once */
    private PurgeScanResult takeScan(User user) {
        PendingScan pending = pendingScans.remove(keyOf(user));
        if (pending == null) {
            return null;
        }
        return System.currentTimeMillis() - pending.createdAt() > SCAN_EXPIRY_MS ? null : pending.scan();
    }

    /**
     * Saves all worlds, runs the scan on an async thread, stores the result
     * for this user and prompts for confirmation if non-empty.
     */
    protected final void runScanAndPrompt(User user, Supplier<PurgeScanResult> scanFn) {
        if (TaskScheduler.isFolia() && !Bukkit.isGlobalTickThread()) {
            TaskScheduler.global(getPlugin(), () -> runScanAndPrompt(user, scanFn));
            return;
        }
        user.sendMessage("commands.admin.purge.scanning");
        getPlugin().log(logPrefix() + ": saving all worlds before scanning...");
        Bukkit.getWorlds().forEach(World::save);
        getPlugin().log(logPrefix() + ": world save complete");

        inPurge = true;
        TaskScheduler.async(getPlugin(), () -> {
            try {
                PurgeScanResult scan = scanFn.get();
                if (scan != null && !scan.isEmpty()) {
                    pendingScans.put(keyOf(user), new PendingScan(scan, System.currentTimeMillis()));
                }
                displayResultsAndPrompt(user, scan);
            } finally {
                inPurge = false;
            }
        });
    }

    /**
     * Confirm path: save worlds, run any pre-delete hook, then dispatch the
     * region-file deletion asynchronously.
     */
    protected final boolean deleteEverything(User user) {
        if (TaskScheduler.isFolia() && !Bukkit.isGlobalTickThread()) {
            // Take the scan now: a second /purge run before this lands would overwrite it,
            // and the admin would then delete a scan they never saw the results of.
            PurgeScanResult scan = takeScan(user);
            TaskScheduler.global(getPlugin(), () -> deleteEverything(user, scan));
            return true;
        }
        return deleteEverything(user, takeScan(user));
    }

    private boolean deleteEverything(User user, PurgeScanResult scan) {
        if (scan == null || scan.isEmpty()) {
            user.sendMessage(NONE_FOUND);
            return false;
        }
        getPlugin().log(logPrefix() + ": saving all worlds before deleting region files...");
        Bukkit.getWorlds().forEach(World::save);
        inPurge = true;
        try {
            beforeDelete(scan);
            getPlugin().log(logPrefix() + ": world save complete, dispatching deletion");
            TaskScheduler.async(getPlugin(), () -> {
                boolean ok = false;
                try {
                    ok = getPlugin().getPurgeRegionsService().delete(scan);
                } finally {
                    // Reset here, not on the user task: the admin may log out while the
                    // deletion runs and Folia drops entity tasks for offline players.
                    inPurge = false;
                    boolean deleteSucceeded = ok;
                    TaskScheduler.user(getPlugin(), user, () -> {
                        if (deleteSucceeded) {
                            // On Folia the delete is only registered and happens at the next start.
                            user.sendMessage(TaskScheduler.isFolia()
                                    ? "commands.admin.purge.deferred" : successMessageKey());
                        } else {
                            getPlugin().log(logPrefix() + ": failed to delete one or more region files");
                            user.sendMessage("commands.admin.purge.failed");
                        }
                    });
                }
            });
            return true;
        } catch (RuntimeException e) {
            inPurge = false;
            throw e;
        }
    }

    private void displayResultsAndPrompt(User user, PurgeScanResult scan) {
        Set<Island> uniqueIslands = scan.deletableRegions().values().stream()
                .flatMap(Set::stream)
                .map(getPlugin().getIslands()::getIslandById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        logScanContents(uniqueIslands, scan);

        if (scan.isEmpty()) {
            TaskScheduler.user(getPlugin(), user, () -> user.sendMessage(NONE_FOUND));
        } else {
            TaskScheduler.user(getPlugin(), user, () -> {
                user.sendMessage("commands.admin.purge.purgable-islands",
                        TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                sendConfirmPrompt(user);
            });
        }
    }

    /** Log prefix used across the scan and delete phases (e.g. {@code "Purge"}). */
    protected abstract String logPrefix();

    /** Locale key sent to the user when delete completes successfully. */
    protected abstract String successMessageKey();

    /** Send any subclass-specific confirmation prompt(s). Runs on the main thread. */
    protected abstract void sendConfirmPrompt(User user);

    /** Hook invoked on the main thread immediately before the async delete. Default: no-op. */
    protected void beforeDelete(PurgeScanResult scan) {
        // no-op
    }

    /** Hook invoked off-thread to log scan contents before the prompt. Default: no-op. */
    protected void logScanContents(Set<Island> uniqueIslands, PurgeScanResult scan) {
        // no-op
    }
}
