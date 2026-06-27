package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.google.common.base.Enums;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;


/**
 * Handles interaction with entities like armor stands
 * Note - armor stand protection from breaking or placing is done elsewhere.
 * @author tastybento
 *
 */
public class EntityInteractListener extends FlagListener {

    /**
     * The Sulfur Cube entity type (Minecraft 26.2), resolved at runtime so the code still
     * compiles against earlier API versions. {@code null} when absent. Non-final so tests can
     * inject a stand-in type (the JVM constant-folds {@code static final} fields).
     */
    @SuppressWarnings("java:S3008") // non-final by design; see Javadoc (test injection)
    private static EntityType SULFUR_CUBE = Enums.getIfPresent(EntityType.class, "SULFUR_CUBE")
            .orNull();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand) {
            checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.ARMOR_STAND);
        }
     }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
    {
        Player p = e.getPlayer();
        Location l = e.getRightClicked().getLocation();

        if (e.getRightClicked() instanceof Vehicle) {
            handleVehicleInteract(e, p, l);
            return;
        }
        if (e.getRightClicked() instanceof Villager && !(e.getRightClicked() instanceof WanderingTrader)) {
            this.checkIsland(e, p, l, Flags.TRADING);
        } else if (e.getRightClicked() instanceof Allay
                || e.getRightClicked().getType().name().equals("COPPER_GOLEM")) {
            this.checkIsland(e, p, l, Flags.ALLAY);
        } else if (SULFUR_CUBE != null && e.getRightClicked().getType() == SULFUR_CUBE) {
            // Sulfur Cube (26.2) absorbs the block the player is holding - treat as placing a block
            Material hand = p.getInventory().getItemInMainHand().getType();
            if (hand.isBlock() && !hand.isAir()) {
                this.checkIsland(e, p, l, Flags.PLACE_BLOCKS);
            }
        }
        checkNameTag(e, p, l);
    }

    private void handleVehicleInteract(PlayerInteractEntityEvent e, Player p, Location l) {
        if (e.getRightClicked() instanceof Animals) {
            this.checkIsland(e, p, l, Flags.RIDING);
        } else if (e.getRightClicked() instanceof RideableMinecart) {
            this.checkIsland(e, p, l, Flags.MINECART);
        } else if (e.getRightClicked() instanceof StorageMinecart
                || (e.getPlayer().isSneaking() && e.getRightClicked() instanceof ChestBoat)) {
            this.checkIsland(e, p, l, Flags.CHEST);
        } else if (e.getRightClicked() instanceof HopperMinecart) {
            this.checkIsland(e, p, l, Flags.HOPPER);
        } else if (e.getRightClicked() instanceof PoweredMinecart) {
            this.checkIsland(e, p, l, Flags.FURNACE);
        } else if (e.getRightClicked() instanceof Boat) {
            this.checkIsland(e, p, l, Flags.BOAT);
        }
    }

    private void checkNameTag(PlayerInteractEntityEvent e, Player p, Location l) {
        if (p.getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {
            this.checkIsland(e, p, l, Flags.NAME_TAG);
        }
    }
}
