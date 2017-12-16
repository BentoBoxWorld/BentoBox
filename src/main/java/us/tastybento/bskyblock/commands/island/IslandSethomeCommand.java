package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class IslandSethomeCommand extends CommandArgument {

    public IslandSethomeCommand() {
        super("sethome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;
        UUID playerUUID = player.getUniqueId();
        if (!player.hasPermission(Settings.PERMPREFIX + "island.sethome")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }
        // Check island
        if (plugin.getIslands().getIsland(player.getUniqueId()) == null) {
            sender.sendMessage(ChatColor.RED + plugin.getLocale(playerUUID).get("general.errors.no-island"));
            return true;
        }
        if (!plugin.getIslands().playerIsOnIsland(player)) {
            sender.sendMessage(ChatColor.RED +  plugin.getLocale(playerUUID).get("sethome.error.NotOnIsland"));
            return true; 
        }
        if (args.length == 0) {
            // island sethome
            plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation());
            sender.sendMessage(ChatColor.GREEN + plugin.getLocale(playerUUID).get("sethome.homeSet"));
        } else if (args.length == 1) {
            // Dynamic home sizes with permissions
            int maxHomes = Util.getPermValue(player, Settings.PERMPREFIX + "island.maxhomes", Settings.maxHomes);
            if (maxHomes > 1) {
                // Check the number given is a number
                int number = 0;
                try {
                    number = Integer.valueOf(args[0]);
                    if (number < 1 || number > maxHomes) {
                        sender.sendMessage(ChatColor.RED +  plugin.getLocale(playerUUID).get("sethome.error.NumHomes").replace("[max]",String.valueOf(maxHomes)));
                    } else {
                        plugin.getPlayers().setHomeLocation(playerUUID, player.getLocation(), number);
                        sender.sendMessage(ChatColor.GREEN + plugin.getLocale(playerUUID).get("sethome.homeSet"));
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + plugin.getLocale(playerUUID).get("sethome.error.NumHomes").replace("[max]",String.valueOf(maxHomes)));
                }
            } else {
                sender.sendMessage(ChatColor.RED + plugin.getLocale(playerUUID).get("general.errors.no-permission"));
            }
        }
        return true;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }
}
