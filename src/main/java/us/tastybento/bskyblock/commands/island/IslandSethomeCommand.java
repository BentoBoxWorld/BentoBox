package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandSethomeCommand extends CompositeCommand {

    public IslandSethomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "sethome");
        this.setPermission(Settings.PERMPREFIX + "island.sethome");
        this.setOnlyPlayer(true);
        this.setUsage("commands.island.sethome.usage");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Check island
        if (getPlugin().getIslands().getIsland(user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return true;
        }
        if (!getPlugin().getIslands().playerIsOnIsland(user.getPlayer())) {
            user.sendMessage("commands.island.sethome.must-be-on-your-island");
            return true; 
        }
        if (args.isEmpty()) {
            // island sethome
            getPlugin().getPlayers().setHomeLocation(playerUUID, user.getLocation());
            user.sendMessage("commands.island.sethome.home-set");
        } else {
            // Dynamic home sizes with permissions
            int maxHomes = Util.getPermValue(user.getPlayer(), Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
            if (maxHomes > 1) {
                // Check the number given is a number
                int number = 0;
                try {
                    number = Integer.valueOf(args.get(0));
                    if (number < 1 || number > maxHomes) {
                        user.sendMessage("commands.island.sethome.num-homes", "[max]", String.valueOf(maxHomes));
                    } else {
                        getPlugin().getPlayers().setHomeLocation(playerUUID, user.getLocation(), number);
                        user.sendMessage("commands.island.sethome.home-set");
                    }
                } catch (Exception e) {
                    user.sendMessage("commands.island.sethome.num-homes", "[max]", String.valueOf(maxHomes));
                }
            } else {
                user.sendMessage("general.errors.no-permission");
            }
        }
        return true;
    }

}
