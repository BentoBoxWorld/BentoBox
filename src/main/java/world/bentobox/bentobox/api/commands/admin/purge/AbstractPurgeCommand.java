package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;

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

    protected volatile boolean inPurge;
    protected boolean toBeConfirmed;
    protected User user;
    protected PurgeScanResult lastScan;

    protected AbstractPurgeCommand(CompositeCommand parent, String label) {
        super(parent, label);
    }

    @Override
    public boolean canExecute(User user, String label, java.util.List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        return true;
    }

    /**
     * Saves all worlds, runs the scan on an async thread, stores the result
     * in {@link #lastScan} and prompts for confirmation if non-empty.
     */
    protected final void runScanAndPrompt(Supplier<PurgeScanResult> scanFn) {
        user.sendMessage("commands.admin.purge.scanning");
        getPlugin().log(logPrefix() + ": saving all worlds before scanning...");
        Bukkit.getWorlds().forEach(World::save);
        getPlugin().log(logPrefix() + ": world save complete");

        inPurge = true;
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                lastScan = scanFn.get();
                displayResultsAndPrompt(lastScan);
            } finally {
                inPurge = false;
            }
        });
    }

    /**
     * Confirm path: save worlds, run any pre-delete hook, then dispatch the
     * region-file deletion asynchronously.
     */
    protected final boolean deleteEverything() {
        if (lastScan == null || lastScan.isEmpty()) {
            user.sendMessage(NONE_FOUND);
            return false;
        }
        PurgeScanResult scan = lastScan;
        lastScan = null;
        toBeConfirmed = false;
        getPlugin().log(logPrefix() + ": saving all worlds before deleting region files...");
        Bukkit.getWorlds().forEach(World::save);
        inPurge = true;
        try {
            beforeDelete(scan);
            getPlugin().log(logPrefix() + ": world save complete, dispatching deletion");
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                boolean ok = false;
                try {
                    ok = getPlugin().getPurgeRegionsService().delete(scan);
                } finally {
                    boolean deleteSucceeded = ok;
                    Bukkit.getScheduler().runTask(getPlugin(), () -> {
                        try {
                            if (deleteSucceeded) {
                                user.sendMessage(successMessageKey());
                            } else {
                                getPlugin().log(logPrefix() + ": failed to delete one or more region files");
                                user.sendMessage("commands.admin.purge.failed");
                            }
                        } finally {
                            inPurge = false;
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

    private void displayResultsAndPrompt(PurgeScanResult scan) {
        Set<Island> uniqueIslands = scan.deletableRegions().values().stream()
                .flatMap(Set::stream)
                .map(getPlugin().getIslands()::getIslandById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        logScanContents(uniqueIslands, scan);

        if (scan.isEmpty()) {
            Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage(NONE_FOUND));
        } else {
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                user.sendMessage("commands.admin.purge.purgable-islands",
                        TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                sendConfirmPrompt();
                toBeConfirmed = true;
            });
        }
    }

    /** Log prefix used across the scan and delete phases (e.g. {@code "Purge"}). */
    protected abstract String logPrefix();

    /** Locale key sent to the user when delete completes successfully. */
    protected abstract String successMessageKey();

    /** Send any subclass-specific confirmation prompt(s). Runs on the main thread. */
    protected abstract void sendConfirmPrompt();

    /** Hook invoked on the main thread immediately before the async delete. Default: no-op. */
    protected void beforeDelete(PurgeScanResult scan) {
        // no-op
    }

    /** Hook invoked off-thread to log scan contents before the prompt. Default: no-op. */
    protected void logScanContents(Set<Island> uniqueIslands, PurgeScanResult scan) {
        // no-op
    }
}
