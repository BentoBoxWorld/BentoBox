/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author ben
 *
 */
public class IslandSetnameCommand extends CompositeCommand {

    public IslandSetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setname");
        this.setPermission(Settings.PERMPREFIX + "island.name");
        this.setOnlyPlayer(true);
        this.setUsage("commands.island.setname.usage");

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        Player player = user.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return true;
        }
        // Explain command
        if (args.isEmpty()) {
            user.sendMessage(getUsage());
            return true;
        }

        // Naming the island - join all the arguments with spaces.
        String name = args.stream().collect(Collectors.joining( " " ));

        // Check if the name isn't too short or too long
        if (name.length() < Settings.nameMinLength) {
            user.sendMessage("commands.island.setname.too-short", "[length]",  String.valueOf(Settings.nameMinLength));
            return true;
        }
        if (name.length() > Settings.nameMaxLength) {
            user.sendMessage("commands.island.setname.too-long", "[length]", String.valueOf(Settings.nameMaxLength));
            return true;
        }

        // Set the name
        if (!player.hasPermission(Settings.PERMPREFIX + "island.name.format"))
            getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
        else getIslands().getIsland(playerUUID).setName(name);

        user.sendMessage("general.success");
        return true;
    }

}
