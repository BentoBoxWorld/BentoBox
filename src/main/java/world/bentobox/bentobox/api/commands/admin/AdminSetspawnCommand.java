package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.World;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Admin command (only player) to set an island as the world's spawn.
 * @author Poslovitch, tastybento
 * @since 1.1
 */
public class AdminSetspawnCommand extends ConfirmableCommand {

    public AdminSetspawnCommand(CompositeCommand parent) {
        super(parent, "setspawn");
    }

    @Override
    public void setup() {
        setPermission("admin.setspawn");
        setOnlyPlayer(true);
        setDescription("commands.admin.setspawn.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Optional<Island> island = getIslands().getIslandAt(user.getLocation());

        if (island.isPresent()) {
            // Check if the island is already a spawn
            if (island.map(Island::isSpawn).orElse(false)) {
                // Show warning, but allow it because the position may change
                user.sendMessage("commands.admin.setspawn.already-spawn");
            }

            // Everything's fine, we can set the island as spawn :)
            askConfirmation(user, user.getTranslation("commands.admin.setspawn.confirmation"), () -> setSpawn(user, island.get()));
            return true;
        } else {
            user.sendMessage("commands.admin.setspawn.no-island-here");
            return false;
        }
    }

    private void setSpawn(User user, Island i) {
        if (!i.getMembers().isEmpty()) {
            if (i.isOwned()) {
                // Build and fire event
                IslandEvent.builder()
                .island(i)
                .location(i.getCenter())
                .reason(IslandEvent.Reason.UNREGISTERED)
                .involvedPlayer(i.getOwner())
                .admin(true)
                .build();
            }
            // If island is owned, then unregister the owner and any members
            new ImmutableSet.Builder<UUID>().addAll(i.getMembers().keySet()).build().forEach(m -> {
                getIslands().removePlayer(getWorld(), m);
                getPlayers().clearHomeLocations(getWorld(), m);
            });
        }
        getIslands().setSpawn(i);
        i.setSpawnPoint(World.Environment.NORMAL, user.getLocation());
        // Set the island's range to the full island space because it is spawn
        i.setProtectionRange(i.getRange());
        user.sendMessage("commands.admin.setspawn.success");
    }
}
