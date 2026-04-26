package world.bentobox.bentobox.api.commands.admin.purge;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.managers.PurgeRegionsService.PurgeScanResult;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Admin command to purge abandoned islands by deleting their region files
 * directly from disk.
 *
 * <p>Since 3.15.0 this command <em>does not</em> soft-delete islands via the
 * DB pipeline the way older versions did — it scans {@code .mca} region files
 * older than {@code <days>}, filters out spawn, purge-protected and still-active
 * islands, and deletes the underlying files. This is what the former
 * {@code /bbox admin purge regions} subcommand used to do; the two have been
 * merged because disk-freeing is the only form of purge that matters.
 *
 * <p>Heavy lifting is delegated to {@link PurgeRegionsService}.
 */
public class AdminPurgeCommand extends AbstractPurgeCommand {

    private static final String IN_WORLD = " in world ";
    private static final String WILL_BE_DELETED = " will be deleted";

    public AdminPurgeCommand(CompositeCommand parent) {
        super(parent, "purge");
    }

    @Override
    public void setup() {
        setPermission("admin.purge");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.parameters");
        setDescription("commands.admin.purge.description");
        new AdminPurgeUnownedCommand(this);
        new AdminPurgeProtectCommand(this);
        new AdminPurgeAgeRegionsCommand(this);
        new AdminPurgeDeletedCommand(this);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!super.canExecute(user, label, args)) {
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
        if (args.getFirst().equalsIgnoreCase("confirm") && toBeConfirmed) {
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

        final int finalDays = days;
        runScanAndPrompt(() -> getPlugin().getPurgeRegionsService().scan(getWorld(), finalDays));
        return true;
    }

    @Override
    protected String logPrefix() {
        return "Purge";
    }

    @Override
    protected String successMessageKey() {
        return "general.success";
    }

    @Override
    protected void sendConfirmPrompt() {
        user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getTopLabel());
        user.sendMessage("general.beta");
    }

    @Override
    protected void logScanContents(Set<Island> uniqueIslands, PurgeScanResult scan) {
        uniqueIslands.forEach(this::displayIsland);
        scan.deletableRegions().entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .forEach(e -> displayEmptyRegion(e.getKey()));
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
