package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Sets the owner of an island.
 * 
 * @author tastybento
 */
public class AdminTeamSetownerCommand extends ConfirmableCommand {

    private @Nullable UUID targetUUID;
    private Island island;
    private @Nullable UUID previousOwnerUUID;

    public AdminTeamSetownerCommand(CompositeCommand parent) {
        super(parent, "setowner");
    }

    @Override
    public void setup() {
        setPermission("mod.team.setowner");
        setParametersHelp("commands.admin.team.setowner.parameters");
        setDescription("commands.admin.team.setowner.description");
        // Not only-player: the island can be named explicitly (second arg) so this runs from the
        // console too, which lets automation (e.g. Skript) transfer ownership.
        this.setOnlyPlayer(false);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Syntax: <new owner> [current island owner]
        if (args.isEmpty() || args.size() > 2) {
            showHelp(this, user);
            return false;
        }

        // Get the new owner
        targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }

        // Work out which island to transfer
        if (args.size() == 2) {
            // Island named by its current owner - location independent, so this works from the console
            UUID islandOwnerUUID = Util.getUUID(args.get(1));
            if (islandOwnerUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(1));
                return false;
            }
            island = getIslands().getIsland(getWorld(), islandOwnerUUID);
            // getIsland() returns the player's active/primary island, which may be a team island
            // they only belong to. Require that they actually own it, so we never transfer the
            // wrong island out from under its real owner.
            if (island == null || !islandOwnerUUID.equals(island.getOwner())) {
                user.sendMessage("general.errors.player-has-no-island");
                return false;
            }
        } else {
            // No island named - use the one the player is standing on. This requires an in-game player.
            if (!user.isPlayer()) {
                user.sendMessage("commands.admin.team.setowner.specify-island");
                return false;
            }
            Optional<Island> opIsland = getIslands().getIslandAt(user.getLocation());
            if (opIsland.isEmpty()) {
                user.sendMessage("commands.admin.team.setowner.must-be-on-island");
                return false;
            }
            island = opIsland.get();
        }
        previousOwnerUUID = island.getOwner();
        if (targetUUID.equals(previousOwnerUUID)) {
            user.sendMessage("commands.admin.team.setowner.already-owner", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Refuse if the recipient would exceed their concurrent-islands cap by gaining this island (#2908).
        // If the target is already a member of this island it is already counted, so discount it -
        // otherwise a member whose only island is this one is wrongly blocked at the limit (#2996).
        User target = User.getInstance(targetUUID);
        int num = this.getIslands().getNumberOfConcurrentIslands(targetUUID, getWorld());
        if (island.inTeam(targetUUID)) {
            num--;
        }
        int max = target.getPermissionValue(
                this.getIWM().getAddon(getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("") + "island.number",
                this.getIWM().getWorldSettings(getWorld()).getConcurrentIslands());
        if (num >= max) {
            user.sendMessage("commands.admin.team.setowner.errors.at-max", TextVariables.NAME, args.getFirst(),
                    TextVariables.NUMBER, String.valueOf(num), "[max]", String.valueOf(max));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(island);
        Objects.requireNonNull(targetUUID);

        // The console (e.g. Skript automation) cannot answer a confirmation prompt, so transfer at once.
        if (!user.isPlayer()) {
            changeOwner(user);
            return true;
        }

        this.askConfirmation(user, user.getTranslation("commands.admin.team.setowner.confirmation", TextVariables.NAME,
                args.getFirst(), TextVariables.XYZ, Util.xyz(island.getCenter().toVector())), () -> changeOwner(user));
        return true;

    }

    protected void changeOwner(User user) {
        assert targetUUID != null;
        User target = User.getInstance(targetUUID);
        // Fire event so add-ons know
        // Call the setowner event
        TeamEvent.builder().island(island).reason(TeamEvent.Reason.SETOWNER).involvedPlayer(targetUUID).admin(true)
                .build();

        // Call the rank change event for the new island owner
        // We need to call it BEFORE the actual change, in order to retain the player's
        // previous rank.
        IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(true)
                .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(island.getRank(target), RanksManager.OWNER_RANK)
                .build();

        // Make new owner
        getIslands().setOwner(user, targetUUID, island, RanksManager.MEMBER_RANK);
        user.sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, target.getName());

        // Report if this made player have more islands than expected
        // Get how many islands this player has
        int num = this.getIslands().getNumberOfConcurrentIslands(targetUUID, getWorld());
        int max = target.getPermissionValue(
                this.getIWM().getAddon(getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("") + "island.number",
                this.getIWM().getWorldSettings(getWorld()).getConcurrentIslands());
        if (num > max) {
            // You cannot make an island
            user.sendMessage("commands.admin.team.setowner.extra-islands", TextVariables.NUMBER, String.valueOf(num),
                    "[max]", String.valueOf(max));
        }

        // Call the rank change event for the old island owner
        if (previousOwnerUUID != null) {
            // We need to call it AFTER the actual change.
            IslandEvent.builder().island(island).involvedPlayer(previousOwnerUUID).admin(true)
                    .reason(IslandEvent.Reason.RANK_CHANGE)
                    .rankChange(RanksManager.OWNER_RANK, island.getRank(previousOwnerUUID)).build();
        }

    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // Both positions - the new owner and the current island owner - are player names
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        List<String> options = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
