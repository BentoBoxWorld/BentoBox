package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Admin debug/testing command that artificially ages {@code .mca} region
 * files in the current gamemode world so they become candidates for the
 * purge regions flow without having to wait real wall-clock time.
 *
 * <p>The purge scanner reads per-chunk timestamps from the region file
 * header, not from file mtime, so {@code touch} cannot fake ageing.
 * This command rewrites that timestamp table in place via
 * {@link world.bentobox.bentobox.managers.PurgeRegionsService#ageRegions(World, int)}.
 *
 * <p>Usage: {@code /<admin> purge age-regions <days>}
 *
 * @since 3.15.0
 */
public class AdminPurgeAgeRegionsCommand extends CompositeCommand implements Listener {

    private volatile boolean running;

    public AdminPurgeAgeRegionsCommand(CompositeCommand parent) {
        super(parent, "age-regions");
        getAddon().registerListener(this);
    }

    @Override
    public void setup() {
        setPermission("admin.purge.age-regions");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.age-regions.parameters");
        setDescription("commands.admin.purge.age-regions.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (running) {
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

        // Flush in-memory chunk state on the main thread before touching
        // the region files — otherwise an auto-save can overwrite our
        // ageing with current timestamps.
        getPlugin().log("Age-regions: saving all worlds before rewriting timestamps...");
        Bukkit.getWorlds().forEach(World::save);
        getPlugin().log("Age-regions: world save complete");

        running = true;
        final int finalDays = days;
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                int count = getPlugin().getPurgeRegionsService().ageRegions(getWorld(), finalDays);
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    user.sendMessage("commands.admin.purge.age-regions.done",
                            TextVariables.NUMBER, String.valueOf(count));
                    getPlugin().log("Age-regions: " + count + " region file(s) aged by "
                            + finalDays + " day(s) in world " + getWorld().getName());
                });
            } finally {
                running = false;
            }
        });
        return true;
    }
}
