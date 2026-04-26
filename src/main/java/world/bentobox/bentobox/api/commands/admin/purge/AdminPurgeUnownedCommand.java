package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
        Set<Island> unowned = getUnownedIslands();
        user.sendMessage("commands.admin.purge.unowned.unowned-islands",
                TextVariables.NUMBER, String.valueOf(unowned.size()));
        if (unowned.isEmpty()) {
            return true;
        }
        // Log locations up front so the admin can see what they are
        // confirming before they confirm.
        unowned.forEach(i -> getPlugin().log("Unowned island at "
                + Util.xyz(i.getCenter().toVector())
                + " in world " + getWorld().getName()
                + " will be flagged deletable"));
        askConfirmation(user, () -> flagDeletable(user, unowned));
        return true;
    }

    private void flagDeletable(User user, Set<Island> unowned) {
        int flagged = 0;
        for (Island island : unowned) {
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

    Set<Island> getUnownedIslands() {
        return getPlugin().getIslands().getIslands().stream()
                .filter(i -> !i.isSpawn())
                .filter(i -> !i.isPurgeProtected())
                .filter(i -> getWorld().equals(i.getWorld()))
                .filter(Island::isUnowned)
                .filter(i -> !i.isDeletable())
                .collect(Collectors.toSet());
    }

}
