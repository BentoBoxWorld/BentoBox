package us.tastybento.bskyblock.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.teleport.SafeTeleportBuilder;

public class AdminTeleportCommand extends CompositeCommand {

    public AdminTeleportCommand(CompositeCommand parent, String tpCommand) {
        super(parent, tpCommand);
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.tp");
        setOnlyPlayer(true);
        setParameters("commands.admin.tp.parameters");
        setDescription("commands.admin.tp.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();

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
            if (getIslands().hasIsland(world, targetUUID) || getIslands().inTeam(world, targetUUID)) {
                Location warpSpot = getIslands().getIslandLocation(world, targetUUID).toVector().toLocation(getPlugin().getIWM().getIslandWorld());
                if (getLabel().equals("tpnether")) {
                    warpSpot = getIslands().getIslandLocation(world, targetUUID).toVector().toLocation(getPlugin().getIWM().getNetherWorld());
                } else if (getLabel().equals("tpend")) {
                    warpSpot = getIslands().getIslandLocation(world, targetUUID).toVector().toLocation(getPlugin().getIWM().getEndWorld());
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
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        options.addAll(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
