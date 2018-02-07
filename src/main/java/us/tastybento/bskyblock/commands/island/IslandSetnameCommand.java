/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

/**
 * @author tastybento
 *
 */
public class IslandSetnameCommand extends CompositeCommand {

    public IslandSetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setname");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.name");
        this.setOnlyPlayer(true);
        this.setParameters("commands.island.setname.parameters");
        this.setDescription("commands.island.setname.description");
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
            return false;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // Explain command
        if (args.isEmpty()) {
            this.getSubCommand("help").ifPresent(help -> help.execute(user, args));
            return false;
        }

        // Naming the island - join all the arguments with spaces.
        String name = args.stream().collect(Collectors.joining( " " ));

        // Check if the name isn't too short or too long
        if (name.length() < getSettings().getNameMinLength()) {
            user.sendMessage("commands.island.setname.too-short", "[length]",  String.valueOf(getSettings().getNameMinLength()));
            return false;
        }
        if (name.length() > getSettings().getNameMaxLength()) {
            user.sendMessage("commands.island.setname.too-long", "[length]", String.valueOf(getSettings().getNameMaxLength()));
            return false;
        }

        // Set the name
        if (!player.hasPermission(Constants.PERMPREFIX + "island.name.format"))
            getIslands().getIsland(player.getUniqueId()).setName(ChatColor.translateAlternateColorCodes('&', name));
        else getIslands().getIsland(playerUUID).setName(name);

        user.sendMessage("general.success");
        return true;
    }

}
