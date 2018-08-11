package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeTeleportBuilder;

public class AdminTeleportCommand extends CompositeCommand {

    public AdminTeleportCommand(CompositeCommand parent, String tpCommand) {
        super(parent, tpCommand);
    }

    @Override
    public void setup() {
        // Permission
        setPermission(getPermissionPrefix() + "admin.tp");
        setOnlyPlayer(true);
        setParametersHelp("commands.admin.tp.parameters");
        setDescription("commands.admin.tp.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return true;
        }

        // Convert name to a UUID
        final UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        } else {
            if (getIslands().hasIsland(getWorld(), targetUUID) || getIslands().inTeam(getWorld(), targetUUID)) {
                Location warpSpot = getIslands().getIslandLocation(getWorld(), targetUUID).toVector().toLocation(getWorld());
                if (getLabel().equals("tpnether")) {
                    warpSpot = getIslands().getIslandLocation(getWorld(), targetUUID).toVector().toLocation(getPlugin().getIWM().getNetherWorld(getWorld()));
                } else if (getLabel().equals("tpend")) {
                    warpSpot = getIslands().getIslandLocation(getWorld(), targetUUID).toVector().toLocation(getPlugin().getIWM().getEndWorld(getWorld()));
                }
                // Other wise, go to a safe spot
                String failureMessage = user.getTranslation("commands.admin.tp.manual", "[location]", warpSpot.getBlockX() + " " + warpSpot.getBlockY() + " "
                        + warpSpot.getBlockZ());
                new SafeTeleportBuilder(getPlugin()).entity(user.getPlayer())
                .location(warpSpot)
                .failureMessage(failureMessage)
                .build();
                return true;
            }
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
