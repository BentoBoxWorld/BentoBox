package world.bentobox.bentobox.api.commands.admin.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.BonusRangeRecord;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Admin command to remove bonus ranges from a player's island.
 * <p>
 * Bonus ranges are static values added to (or subtracted from) the protection
 * range of an island. They are stored in the {@link Island} as
 * {@link BonusRangeRecord}s and are typically granted by addons. Admins need a
 * way to clear them - for example after the addon that granted them has been
 * removed.
 * <p>
 * Usage: {@code /<admin> range removebonus <player> [id]}. With no id, every
 * bonus range is removed. With an id, only the bonus ranges sharing that unique
 * id are removed. The available ids are offered through tab completion so the
 * admin does not have to guess them.
 *
 * @author tastybento
 * @since 3.17.1
 */
public class AdminRangeRemoveBonusCommand extends ConfirmableCommand {

    private Island targetIsland;
    private @Nullable UUID targetUUID;
    private @Nullable String bonusId;

    /**
     * Admin command to remove bonus ranges from a player's island.
     * @param parent - parent range command
     */
    public AdminRangeRemoveBonusCommand(CompositeCommand parent) {
        super(parent, "removebonus");
    }

    @Override
    public void setup() {
        setPermission("admin.range.removebonus");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.range.removebonus.parameters");
        setDescription("commands.admin.range.removebonus.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Expect the player's name and an optional bonus id
        if (args.isEmpty() || args.size() > 2) {
            showHelp(this, user);
            return false;
        }
        // Get target player
        targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Target must have an island in this world
        targetIsland = getIslands().getIsland(getWorld(), targetUUID);
        if (targetIsland == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Nothing to do if there are no bonus ranges at all
        if (targetIsland.getBonusRanges().isEmpty()) {
            user.sendMessage("commands.admin.range.removebonus.no-bonus");
            return false;
        }
        // A specific bonus id was supplied - it must exist on this island
        bonusId = args.size() == 2 ? args.get(1) : null;
        if (bonusId != null && targetIsland.getBonusRangeRecord(bonusId).isEmpty()) {
            user.sendMessage("commands.admin.range.removebonus.unknown-bonus", "[id]", bonusId);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(targetIsland);
        Objects.requireNonNull(targetUUID);
        askConfirmation(user, () -> removeBonusRanges(user, args.getFirst()));
        return true;
    }

    /**
     * Removes the bonus range(s) from the target island. If {@link #bonusId} is
     * set, only that id's bonus ranges are removed, otherwise all of them are.
     * Fires a range change event if the effective protection range changed and
     * notifies the user. The island is persisted automatically via {@code setChanged()}.
     *
     * @param user the admin running the command
     * @param name the target player's name (for the feedback message)
     */
    void removeBonusRanges(User user, String name) {
        int oldRange = targetIsland.getProtectionRange();

        int removed;
        if (bonusId == null) {
            // Remove every bonus range
            removed = targetIsland.getBonusRanges().stream().mapToInt(BonusRangeRecord::getRange).sum();
            targetIsland.clearAllBonusRanges();
            user.sendMessage("commands.admin.range.removebonus.success", TextVariables.NUMBER,
                    String.valueOf(removed), TextVariables.NAME, name);
        } else {
            // Remove only the bonus ranges for the given id
            removed = targetIsland.getBonusRange(bonusId);
            targetIsland.clearBonusRange(bonusId);
            user.sendMessage("commands.admin.range.removebonus.success-id", "[id]", bonusId,
                    TextVariables.NUMBER, String.valueOf(removed), TextVariables.NAME, name);
        }

        // The effective protection range may have changed - notify addons (not cancellable)
        int newRange = targetIsland.getProtectionRange();
        if (oldRange != newRange) {
            IslandEvent.builder()
                    .island(targetIsland)
                    .location(targetIsland.getCenter())
                    .reason(IslandEvent.Reason.RANGE_CHANGE)
                    .involvedPlayer(targetUUID)
                    .admin(true)
                    .protectionRange(newRange, oldRange)
                    .build();
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.size() <= 1) {
            // Don't show every player on the server. Require at least the first letter
            if (lastArg.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), lastArg));
        } else if (args.size() == 2) {
            // Offer the bonus ids that exist on the target player's island
            UUID uuid = Util.getUUID(args.getFirst());
            if (uuid != null) {
                Island island = getIslands().getIsland(getWorld(), uuid);
                if (island != null) {
                    List<String> ids = island.getBonusRanges().stream().map(BonusRangeRecord::getUniqueId).distinct()
                            .toList();
                    return Optional.of(Util.tabLimit(new ArrayList<>(ids), lastArg));
                }
            }
        }
        return Optional.empty();
    }
}
