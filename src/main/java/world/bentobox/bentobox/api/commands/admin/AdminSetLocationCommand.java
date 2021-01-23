package world.bentobox.bentobox.api.commands.admin;


import java.util.List;
import java.util.Optional;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;


/**
 * This command sets the island location. This defines the center of the protected area.
 * The island location can be anywhere inside the island range area. Therefore the protected
 * range can be up to 2x the island range.
 * The island location will change for all environments.
 * @author tastybento
 * @since 1.16.0
 */
public class AdminSetLocationCommand extends ConfirmableCommand
{
    private Location targetLoc;
    private Island island;


    /**
     * Sub-command constructor
     *
     * @param parent - the parent composite command
     */
    public AdminSetLocationCommand(CompositeCommand parent) {
        super(parent, "setlocation");
    }


    @Override
    public void setup()
    {
        this.setPermission("admin.setlocation");
        this.setParametersHelp("commands.admin.setlocation.parameters");
        this.setDescription("commands.admin.setlocation.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() == 3) {
            // Get location
            targetLoc = getLocation(user, args);
        } else {
            targetLoc = new Location(getWorld(), user.getLocation().getBlockX(), user.getLocation().getBlockY(), user.getLocation().getBlockZ());
        }
        if (targetLoc == null) {
            user.sendMessage("commands.admin.setlocation.xyz-error");
            return false;
        }
        Optional<Island> optionalIsland = getIslands().getIslandAt(targetLoc);
        if (!optionalIsland.isPresent()) {
            user.sendMessage("commands.admin.setspawnpoint.no-island-here");
            return false;
        }
        island = optionalIsland.get();
        return true;
    }

    private Location getLocation(User user, List<String> args) {
        try {
            int x = Integer.parseInt(args.get(0));
            int y = Integer.parseInt(args.get(1));
            int z = Integer.parseInt(args.get(2));
            return new Location(getWorld(), x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        String name = getPlayers().getName(island.getOwner());
        user.sendMessage("commands.admin.setlocation.island", TextVariables.XYZ, Util.xyz(island.getCenter().toVector()), TextVariables.NAME, name);
        // Confirm
        this.askConfirmation(user, user.getTranslation("commands.admin.setlocation.confirmation", TextVariables.XYZ, Util.xyz(targetLoc.toVector())),
                () -> this.setLocation(user));
        return true;
    }


    /**
     * Set the island location to the user's location.
     * @param user User who initiate change.
     */
    private void setLocation(User user) {
        try {
            // Set
            island.setLocation(targetLoc);
            user.sendMessage("commands.admin.setlocation.success", TextVariables.XYZ, Util.xyz(targetLoc.toVector()));
        } catch (Exception e) {
            user.sendMessage("commands.admin.setlocation.failure", TextVariables.XYZ, Util.xyz(targetLoc.toVector()));
            getAddon().logError("Island location could not be changed because the island does not exist");
        }

    }
}
