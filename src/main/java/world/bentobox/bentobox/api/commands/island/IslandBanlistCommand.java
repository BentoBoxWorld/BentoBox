package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class IslandBanlistCommand extends CompositeCommand {

    private Island island;

    public IslandBanlistCommand(CompositeCommand islandCommand) {
        super(islandCommand, "banlist", "banned", "bans");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setDescription("commands.island.banlist.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Player issuing the command must have an island
        if (!getIslands().hasIsland(getWorld(), user.getUniqueId()) && !getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        island = getIslands().getIsland(getWorld(), user.getUniqueId());
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
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
            if (line.length() + n.length() >= 41) {
                lines.add(line.toString().trim());
                line.setLength(0);
            }
            line.append(n);
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

        int banLimit = user.getPermissionValue(getPermissionPrefix() + "ban.maxlimit", getIWM().getBanLimit(getWorld()));
        if (banLimit > -1 && island.getBanned().size() < banLimit) {
            user.sendMessage("commands.island.banlist.you-can-ban", TextVariables.NUMBER, String.valueOf(banLimit - island.getBanned().size()));
        }
        return true;
    }

}
