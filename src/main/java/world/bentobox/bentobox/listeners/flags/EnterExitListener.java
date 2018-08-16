/*

 */
package world.bentobox.bentobox.listeners.flags;

import java.util.Optional;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * The Enter/Exit messages flag is a global flag and applies everywhere
 * @author tastybento
 *
 */
public class EnterExitListener extends AbstractFlagListener {

    private static final Vector XZ = new Vector(1,0,1);

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Only process if Enter Exit flags are active, we are in the right world and there is a change in X or Z coords
        if (!getIWM().inWorld(e.getFrom())
                || e.getFrom().toVector().multiply(XZ).equals(e.getTo().toVector().multiply(XZ))
                || !Flags.ENTER_EXIT_MESSAGES.isSetForWorld(e.getFrom().getWorld())) {
            return;
        }

        Optional<Island> from = getIslands().getProtectedIslandAt(e.getFrom());
        Optional<Island> to = getIslands().getProtectedIslandAt(e.getTo());

        /*
         * Options:
         *
         * from = empty, to = island - entering
         * from = island1, to = island2 - leaving 1, entering 2
         * from = island, to = empty - leaving
         * from = empty, to = empty
         * from = island, to = island
         */
        if (from.equals(to)) {
            return;
        }

        User user = User.getInstance(e.getPlayer());

        from.ifPresent(i -> {
            // Fire the IslandExitEvent
            new IslandEvent.IslandEventBuilder()
                    .island(i)
                    .involvedPlayer(user.getUniqueId())
                    .reason(IslandEvent.Reason.EXIT)
                    .admin(false)
                    .location(user.getLocation())
                    .build();

            // Send message if island is owned by someone
            if (i.getOwner() != null) {
                user.notify("protection.flags.ENTER_EXIT_MESSAGES.now-leaving", TextVariables.NAME, (i.getName() != null) ? i.getName() :
                        user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, getPlugin().getPlayers().getName(i.getOwner())));
            }
            // Send message if island is unowned, but has a name
            else if (i.getName() != null) {
                user.notify("protection.flags.ENTER_EXIT_MESSAGES.now-leaving", TextVariables.NAME, i.getName());
            }
        });

        to.ifPresent(i -> {
            // Fire the IslandEnterEvent
            new IslandEvent.IslandEventBuilder()
                    .island(i)
                    .involvedPlayer(user.getUniqueId())
                    .reason(IslandEvent.Reason.ENTER)
                    .admin(false)
                    .location(user.getLocation())
                    .build();

            // Send message if island is owned by someone
            if (i.getOwner() != null) {
                user.notify("protection.flags.ENTER_EXIT_MESSAGES.now-entering", TextVariables.NAME, (i.getName() != null) ? i.getName() :
                        user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, getPlugin().getPlayers().getName(i.getOwner())));
            }
            // Send message if island is unowned, but has a name
            else if (i.getName() != null) {
                user.notify("protection.flags.ENTER_EXIT_MESSAGES.now-entering", TextVariables.NAME, i.getName());
            }
        });
    }
}
