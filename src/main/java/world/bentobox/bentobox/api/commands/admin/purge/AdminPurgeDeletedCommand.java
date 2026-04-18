package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Set;

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
public class AdminPurgeDeletedCommand extends AbstractPurgeCommand {

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
    public boolean execute(User user, String label, List<String> args) {
        this.user = user;
        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("confirm") && toBeConfirmed) {
            return deleteEverything();
        }
        toBeConfirmed = false;
        runScanAndPrompt(() -> getPlugin().getPurgeRegionsService().scanDeleted(getWorld()));
        return true;
    }

    @Override
    protected String logPrefix() {
        return "Purge deleted";
    }

    @Override
    protected String successMessageKey() {
        return "commands.admin.purge.deleted.deferred";
    }

    @Override
    protected void sendConfirmPrompt() {
        user.sendMessage("commands.admin.purge.deleted.confirm", TextVariables.LABEL, this.getLabel());
    }

    @Override
    protected void beforeDelete(PurgeScanResult scan) {
        // Evict in-memory chunks for the target regions on the main thread,
        // otherwise Paper's autosave/unload would re-flush them over the
        // about-to-be-deleted region files (#region-purge bug).
        getPlugin().getPurgeRegionsService().evictChunks(scan);
    }

    @Override
    protected void logScanContents(Set<Island> uniqueIslands, PurgeScanResult scan) {
        uniqueIslands.forEach(island ->
                getPlugin().log("Deletable island at " + Util.xyz(island.getCenter().toVector())
                        + " in world " + getWorld().getName() + " will be reaped"));
    }
}
