package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.island.NewIsland;

/**
 * /island create - Create an island.
 *
 * @author tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * @param islandCommand - parent command
     */
    public IslandCreateCommand(CompositeCommand islandCommand) {
        super(islandCommand, "create", "new");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.create.parameters");
        setDescription("commands.island.create.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (getIslands().hasIsland(getWorld(), user.getUniqueId())
                || getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIWM().getMaxIslands(getWorld()) > 0
                && getIslands().getIslandCount(getWorld()) >= getIWM().getMaxIslands(getWorld())) {
            // There is too many islands in the world :(
            user.sendMessage("commands.island.create.too-many-islands");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Default schem is 'island'
        String name = "island";
        if (!args.isEmpty()) {
            name = args.get(0).toLowerCase(java.util.Locale.ENGLISH);
            // Permission check
            String permission = this.getPermissionPrefix() + "island.create." + name;
            if (!user.hasPermission(permission)) {
                user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, permission);
                return false;
            }
            // Check the schem name exists
            Set<String> validNames = getPlugin().getSchemsManager().get(getWorld()).keySet();
            if (!validNames.contains(name)) {
                user.sendMessage("commands.island.create.unknown-schem");
                return false;
            }

        }
        user.sendMessage("commands.island.create.creating-island");
        try {
            NewIsland.builder()
            .player(user)
            .world(getWorld())
            .reason(Reason.CREATE)
            .name(name)
            .build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
            return false;
        }
        if (getSettings().isResetCooldownOnCreate()) {
            getParent().getSubCommand("reset").ifPresent(resetCommand -> resetCommand.setCooldown(user.getUniqueId(), null, getSettings().getResetCooldown()));
        }
        return true;
    }
}
