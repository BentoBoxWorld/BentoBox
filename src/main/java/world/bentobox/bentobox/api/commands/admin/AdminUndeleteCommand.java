package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Removes the pending-deletion status from the island the admin is standing on
 * and leaves it unowned. Only works on islands that are actually queued for
 * deletion (soft-deleted, awaiting the region-file purge).
 *
 * <p>Shares its restore logic with {@link AdminRegisterCommand} via
 * {@link world.bentobox.bentobox.managers.IslandsManager#undeleteIsland(Island)};
 * the difference is that register assigns the island to a player whereas this
 * command leaves it unowned.
 *
 * @author tastybento
 * @since 3.6.4
 */
public class AdminUndeleteCommand extends CompositeCommand {

    private Island island;

    public AdminUndeleteCommand(CompositeCommand parent) {
        super(parent, "undelete");
    }

    @Override
    public void setup() {
        setPermission("admin.undelete");
        setOnlyPlayer(true);
        setDescription("commands.admin.undelete.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // No arguments expected
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // Check world
        if (!getWorld().equals(user.getWorld())) {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
        // The island must be at this location and be pending deletion
        Optional<Island> opIsland = getIslands().getIslandAt(user.getLocation());
        if (opIsland.isEmpty() || !opIsland.get().isDeletable()) {
            user.sendMessage("commands.admin.undelete.not-in-deletion");
            return false;
        }
        island = opIsland.get();
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(island);
        // Make sure the island is unowned
        island.setOwner(null);
        // Clear the pending-deletion status so the region purge skips it
        getIslands().undeleteIsland(island);
        Location center = island.getCenter();
        user.sendMessage("commands.admin.undelete.undeleted-island", TextVariables.XYZ,
                Util.xyz(center.toVector()));
        user.sendMessage("general.success");
        return true;
    }

}
