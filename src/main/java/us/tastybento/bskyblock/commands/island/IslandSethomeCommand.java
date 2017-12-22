package us.tastybento.bskyblock.commands.island;

import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandSethomeCommand extends CompositeCommand {

    public IslandSethomeCommand(CompositeCommand command) {
        super(command, "sethome");
        this.setPermission(Settings.PERMPREFIX + "island.sethome");
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String[] args) {
        UUID playerUUID = user.getUniqueId();
        // Check island
        if (plugin.getIslands().getIsland(user.getUniqueId()) == null) {
            user.sendMessage("general.errors.no-island");
            return true;
        }
        if (!plugin.getIslands().playerIsOnIsland(user.getPlayer())) {
            user.sendMessage("sethome.error.NotOnIsland");
            return true; 
        }
        if (args.length == 0) {
            // island sethome
            plugin.getPlayers().setHomeLocation(playerUUID, user.getLocation());
            user.sendMessage("sethome.homeSet");
        } else if (args.length == 1) {
            // Dynamic home sizes with permissions
            int maxHomes = Util.getPermValue(user.getPlayer(), Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
            if (maxHomes > 1) {
                // Check the number given is a number
                int number = 0;
                try {
                    number = Integer.valueOf(args[0]);
                    if (number < 1 || number > maxHomes) {
                        user.sendMessage("sethome.error.NumHomes", "[max]", String.valueOf(maxHomes));
                    } else {
                        plugin.getPlayers().setHomeLocation(playerUUID, user.getLocation(), number);
                        user.sendMessage("sethome.homeSet");
                    }
                } catch (Exception e) {
                    user.sendMessage("sethome.error.NumHomes", "[max]", String.valueOf(maxHomes));
                }
            } else {
                user.sendMessage("general.errors.no-permission");
            }
        }
        return true;
    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }
}
