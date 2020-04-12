package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;

/**
 * Forces migration from one database to another
 *
 * @author tastybento
 * @since 1.5.0
 */
public class BentoBoxMigrateCommand extends ConfirmableCommand {

    private static final String MIGRATED = "commands.bentobox.migrate.migrated";

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
            // Migrate BentoBox data
            user.sendMessage("commands.bentobox.migrate.players");
            new Database<>(getPlugin(), Players.class).loadObjects();
            user.sendMessage(MIGRATED);
            user.sendMessage("commands.bentobox.migrate.names");
            new Database<>(getPlugin(), Names.class).loadObjects();
            user.sendMessage(MIGRATED);
            // Migrate addons data
            user.sendMessage("commands.bentobox.migrate.addons");
            getPlugin().getAddonsManager().getDataObjects().forEach(t -> {
                user.sendMessage("commands.bentobox.migrate.class", TextVariables.DESCRIPTION, BentoBox.getInstance().getSettings().getDatabasePrefix() + t.getCanonicalName());
                new Database<>(getPlugin(), t).loadObjects();
                user.sendMessage(MIGRATED);
            });
        });
        return true;
    }
}
