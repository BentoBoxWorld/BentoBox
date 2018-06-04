/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Optional;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;

/**
 * The Enter/Exit messages flag is a global flag and applies everywhere
 * @author tastybento
 *
 */
public class EnterExitListener extends AbstractFlagListener {
    
    private static final Vector XZ = new Vector(1,0,1);
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Only process if Enter Exit flags are active, we are in the right world and there is a change in X or Z coords
        if (!Flags.ENTER_EXIT_MESSAGES.isSet(e.getFrom().getWorld())
                || e.getFrom().toVector().multiply(XZ).equals(e.getTo().toVector().multiply(XZ)) 
                || !getIslandWorldManager().inWorld(e.getFrom())) {
            return;
        }
        Optional<Island> from = this.getIslands().getProtectedIslandAt(e.getFrom());
        Optional<Island> to = this.getIslands().getProtectedIslandAt(e.getTo());
        
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
        from.ifPresent(i -> user.sendMessage("protection.flags.ENTER_EXIT_MESSAGES.now-leaving", "[name]", !i.getName().isEmpty() ? i.getName() :
                user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", "[name]", getPlugin().getPlayers().getName(i.getOwner()))));
        to.ifPresent(i -> user.sendMessage("protection.flags.ENTER_EXIT_MESSAGES.now-entering", "[name]", !i.getName().isEmpty() ? i.getName() :
            user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", "[name]", getPlugin().getPlayers().getName(i.getOwner()))));
    }
}
