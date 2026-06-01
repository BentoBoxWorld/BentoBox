package world.bentobox.bentobox.api.commands.admin.range;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
 * Admin command to remove every bonus range carrying a given id from <b>all</b>
 * islands in this gamemode's world.
 * <p>
 * Addons tag the bonus ranges they grant with their own name (the bonus
 * {@code uniqueId}); for example the Upgrades addon stores them under the id
 * {@code "Upgrades"}. When such an addon is removed, the bonus ranges it added
 * remain on every island it ever touched. This command purges them in one go.
 *
 * @author tastybento
 * @since 3.17.1
 */
public class AdminRangePurgeBonusCommand extends ConfirmableCommand {

    private @Nullable String bonusId;

    /**
     * Admin command to remove a bonus range id from every island.
     * @param parent - parent range command
     */
    public AdminRangePurgeBonusCommand(CompositeCommand parent) {
        super(parent, "purgebonus");
    }

    @Override
    public void setup() {
        setPermission("admin.range.purgebonus");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.range.purgebonus.parameters");
        setDescription("commands.admin.range.purgebonus.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // A single bonus id is expected
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        bonusId = args.get(0);
        // There must be at least one island carrying this bonus id
        if (islandsWithBonus(bonusId).isEmpty()) {
            user.sendMessage("commands.admin.range.purgebonus.none", "[id]", bonusId);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(bonusId);
        // Warn how many islands will be affected before the admin confirms
        int count = islandsWithBonus(bonusId).size();
        user.sendMessage("commands.admin.range.purgebonus.warning", "[id]", bonusId, TextVariables.NUMBER,
                String.valueOf(count));
        final String id = bonusId;
        askConfirmation(user, () -> purge(user, id));
        return true;
    }

    /**
     * Removes the bonus range id from every island that carries it, firing a range
     * change event per island whose effective protection range changed. Each island
     * is persisted automatically via {@code setChanged()}.
     *
     * @param user the admin running the command
     * @param id   the bonus range uniqueId to purge
     */
    void purge(User user, String id) {
        int islandsChanged = 0;
        for (Island island : islandsWithBonus(id)) {
            int oldRange = island.getProtectionRange();
            island.clearBonusRange(id);
            int newRange = island.getProtectionRange();
            if (oldRange != newRange) {
                IslandEvent.builder()
                        .island(island)
                        .location(island.getCenter())
                        .reason(IslandEvent.Reason.RANGE_CHANGE)
                        .involvedPlayer(island.getOwner())
                        .admin(true)
                        .protectionRange(newRange, oldRange)
                        .build();
            }
            islandsChanged++;
        }
        getPlugin().log("Purged bonus range '" + id + "' from " + islandsChanged + " island(s) in "
                + getWorld().getName());
        user.sendMessage("commands.admin.range.purgebonus.success", "[id]", id, TextVariables.NUMBER,
                String.valueOf(islandsChanged));
    }

    /**
     * @return the islands in this world that carry a bonus range with the given id.
     *         Uses the island cache so the live, canonical instances are mutated.
     */
    private List<Island> islandsWithBonus(String id) {
        return getIslands().getIslandCache().getIslands(getWorld()).stream()
                .filter(i -> i.getBonusRangeRecord(id).isPresent()).toList();
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() <= 1) {
            String lastArg = !args.isEmpty() ? args.getLast() : "";
            // Suggest the bonus ids that exist on currently-loaded islands in this world
            Collection<Island> cached = getIslands().getIslandCache().getCachedIslands();
            List<String> ids = cached.stream().filter(i -> getWorld().equals(i.getWorld()))
                    .flatMap(i -> i.getBonusRanges().stream()).map(BonusRangeRecord::getUniqueId).distinct().sorted()
                    .toList();
            return Optional.of(Util.tabLimit(new ArrayList<>(ids), lastArg));
        }
        return Optional.empty();
    }
}
