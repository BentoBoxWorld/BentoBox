package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;

/**
 * Flags every unowned, non-spawn, non-purge-protected island in the current
 * world as {@code deletable} so that the next {@code /bbox admin purge deleted}
 * (or the housekeeping deleted-sweep) reaps their region files.
 *
 * <p>Since 3.15.0 the region-files purge excludes {@code !isOwned()} by design,
 * so orphans never accumulate on disk unless something flags them
 * {@code deletable} first. This command is that bridge.
 */
public class AdminPurgeUnownedCommand extends ConfirmableCommand {

    public AdminPurgeUnownedCommand(AdminPurgeCommand parent) {
        super(parent, "unowned");
    }

    @Override
    public void setup() {
        setPermission("admin.purge.unowned");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.unowned.parameters");
        setDescription("commands.admin.purge.unowned.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // The scan reads every island row in every game mode: keep it off the player's thread.
        getPlugin().getIslands().getIslandsASync().thenAccept(all -> {
            Set<String> unownedIds = all.stream()
                    .filter(i -> !i.isSpawn())
                    .filter(i -> !i.isPurgeProtected())
                    .filter(i -> getWorld().equals(i.getWorld()))
                    .filter(Island::isUnowned)
                    .filter(i -> !i.isDeletable())
                    .map(Island::getUniqueId)
                    .collect(Collectors.toSet());
            TaskScheduler.user(getPlugin(), user, () -> prompt(user, unownedIds));
        }).exceptionally(error -> {
            getPlugin().logError("Could not scan for unowned islands: " + error.getMessage());
            TaskScheduler.user(getPlugin(), user, () -> user.sendMessage("general.errors.general"));
            return null;
        });
        return true;
    }

    private void prompt(User user, Set<String> unowned) {
        user.sendMessage("commands.admin.purge.unowned.unowned-islands",
                TextVariables.NUMBER, String.valueOf(unowned.size()));
        if (unowned.isEmpty()) {
            return;
        }
        askConfirmation(user, () -> flagDeletable(user, unowned));
    }

    private void flagDeletable(User user, Set<String> unowned) {
        int flagged = 0;
        for (String id : unowned) {
            // Re-read the island: it may have been registered to a player or reclaimed by an
            // addon during the 30 second confirmation window.
            Island island = getPlugin().getIslands().getIslandById(id).orElse(null);
            if (island == null || island.isOwned() || island.isSpawn() || island.isPurgeProtected()
                    || island.isDeletable() || !getWorld().equals(island.getWorld())) {
                continue;
            }
            getPlugin().log("Flagging unowned island at "
                    + Util.xyz(island.getCenter().toVector())
                    + " in world " + getWorld().getName() + " as deletable");
            // Reuses the standard soft-delete path: fires the cancellable
            // DELETE event, kicks any trespassers, sets deletable=true,
            // saves, and fires DELETED. An addon veto skips the island.
            getPlugin().getIslands().deleteIsland(island, true, null);
            if (island.isDeletable()) {
                flagged++;
            }
        }
        getPlugin().log("Purge unowned: " + flagged + " of " + unowned.size()
                + " island(s) flagged deletable in " + getWorld().getName());
        user.sendMessage("commands.admin.purge.unowned.flagged",
                TextVariables.NUMBER, String.valueOf(flagged),
                TextVariables.LABEL, getTopLabel());
    }

}
