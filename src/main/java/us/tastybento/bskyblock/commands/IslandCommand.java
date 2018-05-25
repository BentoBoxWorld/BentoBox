package us.tastybento.bskyblock.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.island.IslandAboutCommand;
import us.tastybento.bskyblock.commands.island.IslandBanCommand;
import us.tastybento.bskyblock.commands.island.IslandBanlistCommand;
import us.tastybento.bskyblock.commands.island.IslandCreateCommand;
import us.tastybento.bskyblock.commands.island.IslandGoCommand;
import us.tastybento.bskyblock.commands.island.IslandLanguageCommand;
import us.tastybento.bskyblock.commands.island.IslandResetCommand;
import us.tastybento.bskyblock.commands.island.IslandResetnameCommand;
import us.tastybento.bskyblock.commands.island.IslandSethomeCommand;
import us.tastybento.bskyblock.commands.island.IslandSetnameCommand;
import us.tastybento.bskyblock.commands.island.IslandSettingsCommand;
import us.tastybento.bskyblock.commands.island.IslandUnbanCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamCommand;
import us.tastybento.bskyblock.commands.island.teams.IslandTeamInviteCommand;
import us.tastybento.bskyblock.util.Util;

public class IslandCommand extends CompositeCommand {

    public IslandCommand() {
        super(Constants.ISLANDCOMMAND, "is");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CompositeCommand#setup()
     */
    @Override
    public void setup() {
        setDescription("commands.island.help.description");
        setOnlyPlayer(true);
        // Permission
        setPermission(Constants.PERMPREFIX + "island");
        // Set up subcommands
        new IslandAboutCommand(this);
        new IslandCreateCommand(this);
        new IslandGoCommand(this);
        new IslandResetCommand(this);
        new IslandSetnameCommand(this);
        new IslandResetnameCommand(this);
        new IslandSethomeCommand(this);
        new IslandSettingsCommand(this);
        new IslandLanguageCommand(this);
        new IslandBanCommand(this);
        new IslandUnbanCommand(this);
        new IslandBanlistCommand(this);
        // Team commands
        new IslandTeamCommand(this);
        new IslandTeamInviteCommand(this);

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        if (user == null) {
            return false;
        }
        if (args.isEmpty()) {
            // Check if not in world. If multiple worlds, then tell user to pick one
            if (!getIWM().inWorld(user.getLocation()) && getIWM().getOverWorlds().size() > 1) {
                user.sendMessage("commands.island.help.pick-world", "[worlds]", getIWM().getFriendlyNames());
                return false;
            }
            // If in world, go
            if (getPlugin().getIslands().hasIsland(user.getWorld(), user.getUniqueId())) {
                return getSubCommand("go").map(goCmd -> goCmd.execute(user, new ArrayList<>())).orElse(false);
            }
            // No islands currently
            return getSubCommand("create").map(createCmd -> createCmd.execute(user, new ArrayList<>())).orElse(false);
        } else if (args.size() == 1) {
            // Argument should be a world
            if (getPlugin().getIWM().isOverWorld(args.get(0))) {
                World world = getPlugin().getIWM().getWorld(args.get(0));
                if (getPlugin().getIslands().hasIsland(world, user.getUniqueId())) {
                    return getSubCommand("go").map(goCmd -> goCmd.execute(user, args)).orElse(false);
                }
                // No islands currently
                return getSubCommand("create").map(createCmd -> createCmd.execute(user, args)).orElse(false);
            }
        } 
        user.sendMessage("general.errors.unknown-command", "[label]", Constants.ISLANDCOMMAND);
        return false;

    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {       
        List<String> options = getPlugin().getIWM().getOverWorldNames().stream().collect(Collectors.toList());
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(options, lastArg));
    }
    
}
