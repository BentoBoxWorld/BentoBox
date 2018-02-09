package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.util.SafeSpotTeleport;
import us.tastybento.bskyblock.util.SafeTeleportBuilder;

public class AdminTeleportCommand extends CompositeCommand {

    public AdminTeleportCommand(CompositeCommand parent) {
        super(parent, "tp", "tpnether", "tpend");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.tp");
        setOnlyPlayer(true);
        setDescription("commands.admin.tp.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (args.isEmpty()) {
            user.sendMessage("commands.admin.tp.help");
            return true;
        }

        // Convert name to a UUID
        final UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("errors.unknown-player");
            return false;
        } else {
            if (getPlayers().hasIsland(targetUUID) || getPlayers().inTeam(targetUUID)) {
                Location warpSpot = getIslands().getIslandLocation(targetUUID).toVector().toLocation(getPlugin().getIslandWorldManager().getIslandWorld());
                if (getLabel().equals("tpnether")) {
                    warpSpot = getIslands().getIslandLocation(targetUUID).toVector().toLocation(getPlugin().getIslandWorldManager().getNetherWorld());
                } else if (getLabel().equals("tpend")) {
                    warpSpot = getIslands().getIslandLocation(targetUUID).toVector().toLocation(getPlugin().getIslandWorldManager().getEndWorld());
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
            user.sendMessage("command.admin.tp.no-island");
            return false;
        }
    }

}
