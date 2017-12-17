package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;

public class IslandSethomeCommand extends CommandArgument {

    public IslandSethomeCommand() {
        super("sethome");
    }

    @Override
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage(ChatColor.RED + "general.errors.use-in-game");
            return true;
        }
        UUID playerUUID = user.getUniqueId();
        if (!user.hasPermission(Settings.PERMPREFIX + "island.sethome")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return true;
        }
        // Check island
        if (plugin.getIslands().getIsland(user.getUniqueId()) == null) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return true;
        }
        if (!plugin.getIslands().playerIsOnIsland(user.getPlayer())) {
            user.sendMessage(ChatColor.RED + "sethome.error.NotOnIsland");
            return true; 
        }
        if (args.length == 0) {
            // island sethome
            plugin.getPlayers().setHomeLocation(playerUUID, user.getLocation());
            user.sendMessage(ChatColor.GREEN + "sethome.homeSet");
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
                        user.sendMessage(ChatColor.GREEN + "sethome.homeSet");
                    }
                } catch (Exception e) {
                    user.sendMessage(ChatColor.RED + "sethome.error.NumHomes", "[max]", String.valueOf(maxHomes));
                }
            } else {
                user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            }
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }
}
