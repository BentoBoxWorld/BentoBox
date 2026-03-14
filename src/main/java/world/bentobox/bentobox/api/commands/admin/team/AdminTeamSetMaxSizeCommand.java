package world.bentobox.bentobox.api.commands.admin.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Sets the maximum team size for a player's island, overriding the world default and any
 * permission-based value.
 *
 * <p>Command usage:
 * <ul>
 *     <li><b>/[admin] team maxsize &lt;player&gt; &lt;size&gt;</b> – sets the max team size for the
 *     player's island to {@code size}. Use {@code 0} to reset to the world default.</li>
 * </ul>
 *
 * @author BentoBoxWorld
 * @since 2.7.0
 */
public class AdminTeamSetMaxSizeCommand extends CompositeCommand {

    public AdminTeamSetMaxSizeCommand(CompositeCommand parent) {
        super(parent, "maxsize");
    }

    @Override
    public void setup() {
        setPermission("mod.team.maxsize");
        setParametersHelp("commands.admin.team.maxsize.parameters");
        setDescription("commands.admin.team.maxsize.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        // Resolve target player
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }

        // Validate size argument
        if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }
        int maxSize = Integer.parseInt(args.get(1));

        // Get island
        Island island = getIslands().getPrimaryIsland(getWorld(), targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        // A value of 0 resets to the world default (null means "use world default")
        Integer value = maxSize == 0 ? null : maxSize;
        getIslands().setMaxMembers(island, RanksManager.MEMBER_RANK, value);

        if (value == null) {
            user.sendMessage("commands.admin.team.maxsize.reset",
                    TextVariables.NAME, getPlugin().getPlayers().getName(targetUUID),
                    TextVariables.NUMBER, String.valueOf(getPlugin().getIWM().getMaxTeamSize(getWorld())));
        } else {
            user.sendMessage("commands.admin.team.maxsize.success",
                    TextVariables.NAME, getPlugin().getPlayers().getName(targetUUID),
                    TextVariables.NUMBER, String.valueOf(maxSize));
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 3) {
            return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), args.getLast()));
        }
        return Optional.empty();
    }
}
