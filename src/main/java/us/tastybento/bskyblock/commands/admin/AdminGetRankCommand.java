/**
 * 
 */
package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class AdminGetRankCommand extends CompositeCommand {

    public AdminGetRankCommand(CompositeCommand adminCommand) {
        super(adminCommand, "getrank");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#setup()
     */
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.setrank");
        setOnlyPlayer(false);
        setParameters("commands.admin.getrank.parameters");
        setDescription("commands.admin.getrank.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.BSBCommand#execute(us.tastybento.bskyblock.api.user.User, java.util.List)
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();

        if (args.size() != 1) {
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
        User target = User.getInstance(targetUUID);     
        Island island = getPlugin().getIslands().getIsland(world, targetUUID);       
        int currentRank = island.getRank(target);
        user.sendMessage("commands.admin.getrank.rank-is", "[rank]", user.getTranslation(rm.getRank(currentRank)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
    }
}
