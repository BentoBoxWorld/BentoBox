package us.tastybento.bskyblock.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandBanlistCommand extends CompositeCommand {

    public IslandBanlistCommand(CompositeCommand islandCommand) {
        super(islandCommand, "banlist");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.ban");
        setOnlyPlayer(true);
        setDescription("commands.island.banlist.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        } 
        // Player issuing the command must have an island       
        if (!getIslands().hasIsland(user.getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false; 
        }
        Island island = getIslands().getIsland(user.getWorld(), user.getUniqueId());
        // Show all the players banned on the island
        if (island.getBanned().isEmpty()) {
            user.sendMessage("commands.island.banlist.noone");
            return true;
        }
        // Title
        user.sendMessage("commands.island.banlist.the-following");
        // Create a nicely formatted list
        List<String> names = island.getBanned().stream().map(u -> getPlayers().getName(u)).sorted().collect(Collectors.toList());
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        // Put the names into lines of no more than 40 characters long, separated by commas
        names.forEach(n -> {
            if (line.length() + n.length() < 41) {
                line.append(n);
            } else {
                lines.add(line.toString().trim());
                line.setLength(0);
                line.append(n);
            }
            line.append(", ");
        });
        // Remove trailing comma
        line.setLength(line.length() - 2);
        // Add the final line if it is not empty
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        // Send the strings
        lines.forEach(l -> user.sendMessage("commands.island.banlist.names", "[line]", l));
        return true;
    }

}
