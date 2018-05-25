/**
 * 
 */
package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class AdminSetRankCommand extends CompositeCommand {

    public AdminSetRankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "setrank");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.setrank");
        setOnlyPlayer(false);
        setParameters("commands.admin.setrank.parameters");
        setDescription("commands.admin.setrank.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();

        if (args.size() != 2) {
            // Show help
            showHelp(this, user);
            return false;
        } 
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getPlugin().getIslands().hasIsland(world, targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Get rank
        RanksManager rm = getPlugin().getRanksManager();
        int rankValue = rm.getRanks().entrySet().stream()
                .filter(r -> user.getTranslation(r.getKey()).equalsIgnoreCase(args.get(1))).findFirst()
                .map(r -> r.getValue()).orElse(-999);
        if (rankValue < RanksManager.BANNED_RANK) {
            user.sendMessage("commands.admin.setrank.unknown-rank");
            return false;
        }
        User target = User.getInstance(targetUUID);
        
        Island island = getPlugin().getIslands().getIsland(world, targetUUID);       
        int currentRank = island.getRank(target);
        island.setRank(target, rankValue);
        user.sendMessage("commands.admin.setrank.rank-set", "[from]", user.getTranslation(rm.getRank(currentRank)), "[to]", user.getTranslation(rm.getRank(rankValue)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(getPlugin().getRanksManager().getRanks().keySet().stream().map(user::getTranslation).collect(Collectors.toList()));
    }
}
