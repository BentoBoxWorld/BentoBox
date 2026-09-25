package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects cushions (Minecraft 26.3).
 * <p>
 * A cushion is a block-attached <em>entity</em>, like an item frame, not a block, so none of the
 * block listeners see it. It also is not a {@link org.bukkit.entity.Hanging} or a
 * {@link org.bukkit.entity.Vehicle}, so the hanging and vehicle handlers miss it too:
 * <ul>
 * <li>placing one fires {@link EntityPlaceEvent} &rarr; {@link Flags#PLACE_BLOCKS}</li>
 * <li>hitting one (any damage, including projectiles) destroys it and fires only Paper's
 * {@link EntityBreakByEntityEvent}, never {@code EntityDamageByEntityEvent}
 * &rarr; {@link Flags#BREAK_BLOCKS}</li>
 * <li>sitting on one is a plain {@link PlayerInteractEntityEvent} &rarr; {@link Flags#RIDING}</li>
 * </ul>
 * <b>This class references 26.3-only API in its method signatures, so the JVM refuses to
 * verify it on older servers.</b> It must only be instantiated behind the API-presence check in
 * {@link world.bentobox.bentobox.listeners.BentoBoxListenerRegistrar}; nothing else may name it.
 *
 * @author tastybento
 * @since 3.23.1
 */
public class CushionListener extends FlagListener {

    /**
     * Placing a cushion item spawns the entity - treat it like placing a block.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCushionPlace(final EntityPlaceEvent e) {
        if (e.getEntity() instanceof Cushion cushion && e.getPlayer() != null) {
            checkIsland(e, e.getPlayer(), cushion.getLocation(), Flags.PLACE_BLOCKS);
        }
    }

    /**
     * A cushion breaks on any hit, by hand or projectile. Only player-caused breaks are checked.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCushionBreak(final EntityBreakByEntityEvent e) {
        if (!(e.getEntity() instanceof Cushion cushion)) {
            return;
        }
        Location l = cushion.getLocation();
        if (e.getRemover() instanceof Player p) {
            checkIsland(e, p, l, Flags.BREAK_BLOCKS);
        } else if (e.getRemover() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            checkIsland(e, shooter, l, Flags.BREAK_BLOCKS);
        }
    }

    /**
     * Right-clicking a cushion sits on it - the same kind of state-free mounting as boarding a
     * boat or riding an animal, so it shares {@link Flags#RIDING}.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCushionSit(final PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Cushion cushion) {
            checkIsland(e, e.getPlayer(), cushion.getLocation(), Flags.RIDING);
        }
    }
}
