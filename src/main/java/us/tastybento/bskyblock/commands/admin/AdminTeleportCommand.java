package us.tastybento.bskyblock.commands.admin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.util.SafeTeleportBuilder;
import us.tastybento.bskyblock.util.Util;

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
    
    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = (!args.isEmpty() ? args.getLast() : "");
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        options.addAll(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
