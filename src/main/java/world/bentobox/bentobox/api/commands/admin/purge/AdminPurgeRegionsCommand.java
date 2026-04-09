package world.bentobox.bentobox.api.commands.admin.purge;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Admin command to scan and delete old region files in the gamemode world.
 *
 * <p>Heavy lifting (scanning, filtering, deletion) is delegated to
 * {@link PurgeRegionsService}. This command owns the two-step confirmation
 * flow and the per-island display messages.
 */
public class AdminPurgeRegionsCommand extends CompositeCommand implements Listener {

    private static final String NONE_FOUND = "commands.admin.purge.none-found";
    private static final String IN_WORLD = " in world ";
    private static final String WILL_BE_DELETED = " will be deleted";

    private volatile boolean inPurge;
    private boolean toBeConfirmed;
    private User user;
    private PurgeScanResult lastScan;

    public AdminPurgeRegionsCommand(CompositeCommand parent) {
        super(parent, "regions");
        getAddon().registerListener(this);
    }

    @Override
    public void setup() {
        setPermission("admin.purge.regions");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.regions.parameters");
        setDescription("commands.admin.purge.regions.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.user = user;
        if (args.getFirst().equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            return deleteEverything();
        }
        toBeConfirmed = false;

        int days;
        try {
            days = Integer.parseInt(args.getFirst());
            if (days <= 0) {
                user.sendMessage("commands.admin.purge.days-one-or-more");
                return false;
            }
        } catch (NumberFormatException e) {
            user.sendMessage("commands.admin.purge.days-one-or-more");
            return false;
        }

        user.sendMessage("commands.admin.purge.scanning");
        // Save all worlds to update any region files
        Bukkit.getWorlds().forEach(World::save);

        inPurge = true;
        final int finalDays = days;
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                PurgeRegionsService service = getPlugin().getPurgeRegionsService();
                lastScan = service.scan(getWorld(), finalDays);
                displayResultsAndPrompt(lastScan);
            } finally {
                inPurge = false;
            }
        });
        return true;
    }

    private boolean deleteEverything() {
        if (lastScan == null || lastScan.isEmpty()) {
            user.sendMessage(NONE_FOUND);
            return false;
        }
        PurgeScanResult scan = lastScan;
        lastScan = null;
        toBeConfirmed = false;
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            boolean ok = getPlugin().getPurgeRegionsService().delete(scan);
            Bukkit.getScheduler().runTask(getPlugin(), () ->
                user.sendMessage(ok ? "general.success" : NONE_FOUND));
        });
        return true;
    }

    private void displayResultsAndPrompt(PurgeScanResult scan) {
        Set<Island> uniqueIslands = scan.deleteableRegions().values().stream()
                .flatMap(Set::stream)
                .map(getPlugin().getIslands()::getIslandById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        uniqueIslands.forEach(this::displayIsland);

        scan.deleteableRegions().entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .forEach(e -> displayEmptyRegion(e.getKey()));

        if (scan.isEmpty()) {
            Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage(NONE_FOUND));
        } else {
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                user.sendMessage("commands.admin.purge.purgable-islands",
                        TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                user.sendMessage("commands.admin.purge.regions.confirm",
                        TextVariables.LABEL, this.getLabel());
                user.sendMessage("general.beta"); // TODO Remove beta in the future
                this.toBeConfirmed = true;
            });
        }
    }

    private void displayIsland(Island island) {
        if (island.isDeletable()) {
            getPlugin().log("Deletable island at " + Util.xyz(island.getCenter().toVector())
                    + IN_WORLD + getWorld().getName() + WILL_BE_DELETED);
            return;
        }
        if (island.getOwner() == null) {
            getPlugin().log("Unowned island at " + Util.xyz(island.getCenter().toVector())
                    + IN_WORLD + getWorld().getName() + WILL_BE_DELETED);
            return;
        }
        getPlugin().log("Island at " + Util.xyz(island.getCenter().toVector()) + IN_WORLD + getWorld().getName()
                + " owned by " + getPlugin().getPlayers().getName(island.getOwner())
                + " who last logged in "
                + formatLocalTimestamp(getPlugin().getPlayers().getLastLoginTimestamp(island.getOwner()))
                + WILL_BE_DELETED);
    }

    private void displayEmptyRegion(Pair<Integer, Integer> region) {
        getPlugin().log("Empty region at r." + region.x() + "." + region.z() + IN_WORLD
                + getWorld().getName() + " will be deleted (no islands)");
    }

    private String formatLocalTimestamp(Long millis) {
        if (millis == null) {
            return "(unknown or never recorded)";
        }
        Instant instant = Instant.ofEpochMilli(millis);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
}
