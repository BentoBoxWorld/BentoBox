package world.bentobox.bentobox.api.commands.admin;


import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;


/**
 * This command sets the center of the protected area.
 * The location can be anywhere inside the island range area. Therefore, the protected
 * range can be up to 2x the island range.
 * The location will change for all environments.
 * @author tastybento
 * @since 1.16.0
 */
public class AdminSetProtectionCenterCommand extends ConfirmableCommand
{
    private record ProtectionTarget(Island island, Location location) { }


    /**
     * Sub-command constructor
     *
     * @param parent - the parent composite command
     */
    public AdminSetProtectionCenterCommand(CompositeCommand parent) {
        super(parent, "setprotectionlocation");
    }


    @Override
    public void setup()
    {
        this.setPermission("admin.setprotectionlocation");
        this.setParametersHelp("commands.admin.setprotectionlocation.parameters");
        this.setDescription("commands.admin.setprotectionlocation.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return resolveTarget(user, args) != null;
    }

    private @Nullable ProtectionTarget resolveTarget(User user, List<String> args) {
        if (!args.isEmpty() && args.size() != 3) {
            user.sendMessage("commands.admin.setprotectionlocation.xyz-error");
            return null;
        }
        Location targetLoc;
        if (args.size() == 3) {
            // Get location
            targetLoc = getLocation(args);
        } else {
            if (!user.isPlayer()) {
                user.sendMessage("general.errors.use-in-game");
                return null;
            }
            targetLoc = new Location(getWorld(), user.getLocation().getBlockX(), user.getLocation().getBlockY(), user.getLocation().getBlockZ());
        }
        if (targetLoc == null) {
            user.sendMessage("commands.admin.setprotectionlocation.xyz-error");
            return null;
        }
        Optional<Island> optionalIsland = getIslands().getIslandAt(targetLoc);
        if (optionalIsland.isEmpty()) {
            user.sendMessage("commands.admin.setspawnpoint.no-island-here");
            return null;
        }
        return new ProtectionTarget(optionalIsland.get(), targetLoc);
    }

    private Location getLocation(List<String> args) {
        try {
            int x = Integer.parseInt(args.getFirst());
            int y = Integer.parseInt(args.get(1));
            int z = Integer.parseInt(args.get(2));
            return new Location(getWorld(), x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        ProtectionTarget target = resolveTarget(user, args);
        if (target == null) return false;
        Island island = target.island();
        Location targetLoc = target.location();
        String name = getPlayers().getName(island.getOwner());
        user.sendMessage("commands.admin.setprotectionlocation.island", TextVariables.XYZ, Util.xyz(island.getCenter().toVector()), TextVariables.NAME, name);
        // Confirm
        this.askConfirmation(user, user.getTranslation("commands.admin.setprotectionlocation.confirmation", TextVariables.XYZ, Util.xyz(targetLoc.toVector())),
                () -> this.setLocation(user, island, targetLoc));
        return true;
    }


    /**
     * Set the island location to the user's location.
     * @param user User who initiate change.
     * @param island the confirmed island
     * @param targetLoc the confirmed location
     */
    private void setLocation(User user, Island island, Location targetLoc) {
        if (getIslands().getIslandAt(targetLoc).filter(island::equals).isEmpty()) {
            user.sendMessage("commands.admin.setspawnpoint.no-island-here");
            return;
        }
        try {
            // Set
            island.setProtectionCenter(targetLoc);
            user.sendMessage("commands.admin.setprotectionlocation.success", TextVariables.XYZ, Util.xyz(targetLoc.toVector()));
        } catch (Exception e) {
            user.sendMessage("commands.admin.setprotectionlocation.failure", TextVariables.XYZ, Util.xyz(targetLoc.toVector()));
            getPlugin().logError("Protection location could not be changed: " + e.getMessage());
        }

    }
}
