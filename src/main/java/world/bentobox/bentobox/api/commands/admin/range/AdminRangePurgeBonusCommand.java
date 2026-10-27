package world.bentobox.bentobox.api.commands.admin.range;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.BonusRangeRecord;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * Admin command to remove every bonus range carrying a given id from <b>all</b>
 * islands in this gamemode's world.
 * <p>
 * Addons tag the bonus ranges they grant with their own name (the bonus
 * {@code uniqueId}); for example the Upgrades addon stores them under the id
 * {@code "Upgrades"}. When such an addon is removed, the bonus ranges it added
 * remain on every island it ever touched. This command purges them in one go.
 * <p>
 * Because a server can hold a large number of islands, the scan that finds the
 * affected islands runs asynchronously (off the main thread). The admin is then
 * shown the count and must re-run the command with {@code confirm} to apply it.
 * The actual mutation and event firing happen back on the main thread, on the
 * live cached island instances.
 *
 * @author tastybento
 * @since 3.17.1
 */
public class AdminRangePurgeBonusCommand extends CompositeCommand {

    /** True while an async scan is running, to prevent overlapping runs. */
    volatile boolean inPurge;

    /** Console has no UUID, so it gets its own fixed key. */
    private static final UUID CONSOLE_KEY = new UUID(0L, 0L);
    /** A pending confirmation older than this must be re-scanned. */
    private static final long EXPIRY_MS = java.time.Duration.ofMinutes(10).toMillis();

    private record Pending(String id, List<String> islandIds, long createdAt) {
    }

    /**
     * Pending purge per admin. Command instances are shared server-wide, so a single field
     * would let one admin confirm (and purge) another admin's scan.
     */
    private final java.util.Map<UUID, Pending> pending = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (inPurge) {
            user.sendMessage("commands.admin.range.purgebonus.in-progress");
            return false;
        }
        // Expect "<id>" or "<id> confirm"
        if (args.isEmpty() || args.size() > 2
                || (args.size() == 2 && !args.get(1).equalsIgnoreCase("confirm"))) {
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        String id = args.get(0);
        boolean confirm = args.size() == 2 && args.get(1).equalsIgnoreCase("confirm");
        // Apply this admin's own pending purge if this is the matching confirmation
        if (confirm) {
            Pending p = takePending(user);
            if (p != null && id.equals(p.id())) {
                applyPurge(user, id, p.islandIds());
                return true;
            }
        }
        // Otherwise (re)scan asynchronously and prompt for confirmation
        inPurge = true;
        getPlugin().getIslands().getIslandsASync().thenAccept(all -> {
            List<String> ids = findIslandIds(all, id);
            // Reset here, not on the user task: the admin may log out and Folia drops
            // entity tasks for offline players, which would leave inPurge stuck on.
            inPurge = false;
            TaskScheduler.user(getPlugin(), user, () -> {
                if (ids.isEmpty()) {
                    user.sendMessage("commands.admin.range.purgebonus.none", "[id]", id);
                    return;
                }
                pending.put(keyOf(user), new Pending(id, ids, System.currentTimeMillis()));
                user.sendMessage("commands.admin.range.purgebonus.warning", "[id]", id, TextVariables.NUMBER,
                        String.valueOf(ids.size()));
                user.sendMessage("commands.admin.range.purgebonus.confirm");
            });
        }).exceptionally(ex -> {
            getPlugin().logStacktrace(ex);
            inPurge = false;
            TaskScheduler.user(getPlugin(), user,
                    () -> user.sendMessage("commands.admin.range.purgebonus.failed"));
            return null;
        });
        return true;
    }

    private static UUID keyOf(User user) {
        UUID id = user.getUniqueId();
        return id == null ? CONSOLE_KEY : id;
    }

    /** @return this admin's unexpired pending purge, removing it so it can only be confirmed once */
    private @Nullable Pending takePending(User user) {
        Pending value = pending.remove(keyOf(user));
        if (value == null) {
            return null;
        }
        return System.currentTimeMillis() - value.createdAt() > EXPIRY_MS ? null : value;
    }

    /**
     * @return the unique ids of the islands in this world that carry a bonus range
     *         with the given id.
     */
    List<String> findIslandIds(Collection<Island> islands, String id) {
        return islands.stream().filter(i -> getWorld().equals(i.getWorld()))
                .filter(i -> i.getBonusRangeRecord(id).isPresent()).map(Island::getUniqueId).toList();
    }

    /**
     * Removes the bonus range id from every still-matching island, firing a range
     * change event per island whose effective protection range changed. Runs on the
     * main thread and operates on the live cached island instances, which are
     * persisted automatically via {@code setChanged()}.
     *
     * @param user the admin running the command
     * @param id   the bonus range uniqueId to purge
     * @param ids  the unique ids of the islands found during the async scan
     */
    void applyPurge(User user, String id, List<String> ids) {
        int changed = 0;
        for (String uid : ids) {
            Island island = getIslands().getIslandById(uid).orElse(null);
            // Re-check on the live instance in case it changed since the scan
            if (island == null || island.getBonusRangeRecord(id).isEmpty()) {
                continue;
            }
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
            changed++;
        }
        getPlugin().log("Purged bonus range '" + id + "' from " + changed + " island(s) in "
                + getWorld().getName());
        user.sendMessage("commands.admin.range.purgebonus.success", "[id]", id, TextVariables.NUMBER,
                String.valueOf(changed));
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
        } else if (args.size() == 2) {
            return Optional.of(Util.tabLimit(new ArrayList<>(List.of("confirm")), args.getLast()));
        }
        return Optional.empty();
    }
}
