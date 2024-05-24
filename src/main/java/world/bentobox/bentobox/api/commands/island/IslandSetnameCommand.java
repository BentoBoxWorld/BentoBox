package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;


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
        setPermission("island.name");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.setname.parameters");
        setDescription("commands.island.setname.description");
        setConfigurableRankCommand();
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        // Explain command
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        Island island = getIslands().getIsland(getWorld(), user);

        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // Check command rank.
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Check if the name isn't too short or too long
        if (name.length() < getSettings().getNameMinLength() || ChatColor.stripColor(name).isEmpty()) {
            user.sendMessage("commands.island.setname.name-too-short", TextVariables.NUMBER, String.valueOf(getSettings().getNameMinLength()));
            return false;
        }
        if (name.length() > getSettings().getNameMaxLength()) {
            user.sendMessage("commands.island.setname.name-too-long", TextVariables.NUMBER, String.valueOf(getSettings().getNameMaxLength()));
            return false;
        }

        // Apply colors
        if (user.hasPermission(getPermissionPrefix() + "island.name.format")) {
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Apply colors
        if (user.hasPermission(getPermissionPrefix() + "island.name.format")) {
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        // Everything's good!
        Island island = Objects.requireNonNull(getIslands().getIsland(getWorld(), user));
        String previousName = island.getName();
        island.setName(name);
        user.sendMessage("commands.island.setname.success", TextVariables.NAME, name);
        // Fire the IslandNameEvent
        new IslandEvent.IslandEventBuilder()
        .island(island)
        .involvedPlayer(user.getUniqueId())
        .reason(IslandEvent.Reason.NAME)
        .previousName(previousName)
        .admin(false)
        .build();
        return true;
    }
}
