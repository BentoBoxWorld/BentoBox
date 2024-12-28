package world.bentobox.bentobox.blueprints;

import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import world.bentobox.bentobox.BentoBox;

/**
 * Provides a listener for the Display Objects pasted when a hologram is interacted with
 * https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/player/PlayerInteractAtEntityEvent.html
 */
public class DisplayListener implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) event.getRightClicked();
            NamespacedKey key = new NamespacedKey(BentoBox.getInstance(), "associatedDisplayEntity");

            if (armorStand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String displayEntityUUID = armorStand.getPersistentDataContainer().get(key, PersistentDataType.STRING);

                // Fetch the associated DisplayEntity by UUID
                World world = armorStand.getWorld();
                world.getEntitiesByClass(Display.class).stream()
                        .filter(e -> e.getUniqueId().equals(UUID.fromString(displayEntityUUID))).findFirst()
                        .ifPresent(e -> {
                            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_GLASS_BREAK, 1F,
                                    1F);
                            e.remove();

                        });
            }
        }
    }
}
