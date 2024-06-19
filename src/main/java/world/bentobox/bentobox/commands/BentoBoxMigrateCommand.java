package world.bentobox.bentobox.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 * Forces migration from one database to another
 *
 * @author tastybento
 * @since 1.5.0
 */
public class BentoBoxMigrateCommand extends ConfirmableCommand {

    private static final String MIGRATED = "commands.bentobox.migrate.migrated";
    private Queue<Class<? extends DataObject>> classQueue;
    private BukkitTask task;

    /**
     * Reloads settings, addons and localization command
     * @param parent command parent
     */
    public BentoBoxMigrateCommand(CompositeCommand parent) {
        super(parent, "migrate");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.migrate");
        setDescription("commands.bentobox.migrate.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.askConfirmation(user, () -> {
            user.sendMessage("commands.bentobox.migrate.addons");
            Set<Class<? extends DataObject>> classSet = getPlugin().getAddonsManager().getDataObjects();
            classSet.addAll(Database.getDataobjects());
            // Put classSet into classQueue
            classQueue = new LinkedList<>(classSet);
            // Start a scheduler to step through these in a reasonable time
            task = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
                Class<? extends DataObject> t = classQueue.poll();
                if (t != null) {
                    user.sendMessage("commands.bentobox.migrate.class", TextVariables.DESCRIPTION,
                            BentoBox.getInstance().getSettings().getDatabasePrefix() + t.getCanonicalName());
                    new Database<>(getPlugin(), t).loadObjects();
                    user.sendMessage(MIGRATED);
                } else {
                    user.sendMessage("commands.bentobox.migrate.completed");
                    task.cancel();
                }
            }, 0, 20L);
        });
        return true;
    }
}
