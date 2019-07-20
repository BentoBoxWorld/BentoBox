package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import org.apache.commons.lang.math.NumberUtils;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
public class IslandGoCommand extends DelayedTeleportCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setDescription("commands.island.go.description");
        new CustomIslandMultiHomeHelp(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (getIslands().getIsland(getWorld(), user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if ((getIWM().inWorld(user.getWorld()) && Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()))
                && user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }
        if (!args.isEmpty() && NumberUtils.isDigits(args.get(0))) {
            int homeValue = Integer.parseInt(args.get(0));
            int maxHomes = user.getPermissionValue(getPermissionPrefix() + "island.maxhomes", getIWM().getMaxHomes(getWorld()));
            if (homeValue > 1 && homeValue <= maxHomes) {
                this.delayCommand(user, () -> getIslands().homeTeleport(getWorld(), user.getPlayer(), homeValue));
                return true;
            }
        }
        this.delayCommand(user, () -> getIslands().homeTeleport(getWorld(), user.getPlayer()));
        return true;
    }

}
