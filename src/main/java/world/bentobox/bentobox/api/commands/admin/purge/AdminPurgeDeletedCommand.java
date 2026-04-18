package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.util.Util;

/**
 * Admin command to reap region files for every island already flagged as
 * {@code deletable}, ignoring region-file age entirely.
 *
 * <p>Counterpart to {@link AdminPurgeCommand} which filters on the age of
 * the .mca files. This command trusts the {@code deletable} flag set by
 * {@code /is reset} (and Phase 2 soft-delete) and reaps immediately.
 *
 * <p>Heavy lifting is delegated to {@link PurgeRegionsService#scanDeleted(World)}
 * and {@link PurgeRegionsService#delete(PurgeScanResult)}.
 *
 * @since 3.15.0
 */
public class AdminPurgeDeletedCommand extends CompositeCommand {

    private static final String NONE_FOUND = "commands.admin.purge.none-found";

    private volatile boolean inPurge;
    private boolean toBeConfirmed;
    private User user;
    private PurgeScanResult lastScan;

    public AdminPurgeDeletedCommand(CompositeCommand parent) {
        super(parent, "deleted");
    }

    @Override
    public void setup() {
        setPermission("admin.purge.deleted");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.deleted.parameters");
        setDescription("commands.admin.purge.deleted.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.user = user;
        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("confirm")
                && toBeConfirmed && this.user.equals(user)) {
            return deleteEverything();
        }
        toBeConfirmed = false;

        user.sendMessage("commands.admin.purge.scanning");
        // Save all worlds to flush in-memory chunk state before scanning.
        getPlugin().log("Purge deleted: saving all worlds before scanning...");
        Bukkit.getWorlds().forEach(World::save);
        getPlugin().log("Purge deleted: world save complete");

        inPurge = true;
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                PurgeRegionsService service = getPlugin().getPurgeRegionsService();
                lastScan = service.scanDeleted(getWorld());
                displayResultsAndPrompt(lastScan);
            } finally {
                inPurge = false;
            }
        });
        return true;
    }

    private boolean deleteEverything() {
        if (lastScan == null || lastScan.isEmpty()) {
            user.sendMessage(NONE_FOUND);
            return false;
        }
        PurgeScanResult scan = lastScan;
        lastScan = null;
        toBeConfirmed = false;
        getPlugin().log("Purge deleted: saving all worlds before deleting region files...");
        Bukkit.getWorlds().forEach(World::save);
        // Evict in-memory chunks for the target regions on the main thread,
        // otherwise Paper's autosave/unload would re-flush them over the
        // about-to-be-deleted region files (#region-purge bug).
        getPlugin().getPurgeRegionsService().evictChunks(scan);
        getPlugin().log("Purge deleted: world save complete, dispatching deletion");
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            boolean ok = getPlugin().getPurgeRegionsService().delete(scan);
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                if (ok) {
                    user.sendMessage("commands.admin.purge.deleted.deferred");
                } else {
                    getPlugin().log("Purge deleted: failed to delete one or more region files after a non-empty scan");
                    user.sendMessage("commands.admin.purge.failed");
                }
            });
        });
        return true;
    }

    private void displayResultsAndPrompt(PurgeScanResult scan) {
        Set<Island> uniqueIslands = scan.deletableRegions().values().stream()
                .flatMap(Set::stream)
                .map(getPlugin().getIslands()::getIslandById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        uniqueIslands.forEach(island ->
                getPlugin().log("Deletable island at " + Util.xyz(island.getCenter().toVector())
                        + " in world " + getWorld().getName() + " will be reaped"));

        if (scan.isEmpty()) {
            Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage(NONE_FOUND));
        } else {
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                user.sendMessage("commands.admin.purge.purgable-islands",
                        TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                user.sendMessage("commands.admin.purge.deleted.confirm",
                        TextVariables.LABEL, this.getLabel());
                this.toBeConfirmed = true;
            });
        }
    }
}
