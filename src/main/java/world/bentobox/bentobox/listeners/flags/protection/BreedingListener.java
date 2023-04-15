package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Enums;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles breeding protection
 * Note - animal protection is done elsewhere.
 * @author tastybento
 *
 */
public class BreedingListener extends FlagListener {

    /**
     * A list of items that cause breeding if a player has them in their hand and they click an animal
     * This list may need to be extended with future versions of Minecraft.
     * See this Minecraft Wiki page for reference: <a href="https://minecraft.gamepedia.com/Breeding#Breeding_foods">...</a>.
     */
    private static final Map<EntityType, List<Material>> BREEDING_ITEMS;
    static {
        Map<EntityType, List<Material>> bi = new EnumMap<>(EntityType.class);

        bi.put(EntityType.HORSE, Arrays.asList(Material.GOLDEN_APPLE, Material.GOLDEN_CARROT));
        bi.put(EntityType.DONKEY, Arrays.asList(Material.GOLDEN_APPLE, Material.GOLDEN_CARROT));
        bi.put(EntityType.COW, Collections.singletonList(Material.WHEAT));
        bi.put(EntityType.MUSHROOM_COW, Collections.singletonList(Material.WHEAT));
        bi.put(EntityType.SHEEP, Collections.singletonList(Material.WHEAT));
        bi.put(EntityType.PIG, Arrays.asList(Material.CARROT, Material.POTATO, Material.BEETROOT));
        bi.put(EntityType.CHICKEN, Arrays.asList(Material.WHEAT_SEEDS, Material.PUMPKIN_SEEDS, Material.MELON_SEEDS, Material.BEETROOT_SEEDS));
        bi.put(EntityType.WOLF, Arrays.asList(Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.BEEF, Material.COOKED_BEEF,
                Material.CHICKEN, Material.COOKED_CHICKEN, Material.RABBIT, Material.COOKED_RABBIT,
                Material.MUTTON, Material.COOKED_MUTTON, Material.ROTTEN_FLESH));
        bi.put(EntityType.CAT, Arrays.asList(Material.COD, Material.SALMON));
        bi.put(EntityType.OCELOT, Arrays.asList(Material.COD, Material.SALMON));
        bi.put(EntityType.RABBIT, Arrays.asList(Material.DANDELION, Material.CARROT, Material.GOLDEN_CARROT));
        bi.put(EntityType.LLAMA, Collections.singletonList(Material.HAY_BLOCK));
        bi.put(EntityType.TRADER_LLAMA, Collections.singletonList(Material.HAY_BLOCK));
        bi.put(EntityType.TURTLE, Collections.singletonList(Material.SEAGRASS));
        bi.put(EntityType.PANDA, Collections.singletonList(Material.BAMBOO));
        bi.put(EntityType.FOX, Collections.singletonList(Material.SWEET_BERRIES));
        // 1.15+
        bi.put(EntityType.BEE, Arrays.asList(Material.SUNFLOWER, Material.ORANGE_TULIP, Material.PINK_TULIP,
                Material.RED_TULIP, Material.WHITE_TULIP, Material.ALLIUM,
                Material.AZURE_BLUET, Material.BLUE_ORCHID, Material.CORNFLOWER,
                Material.DANDELION, Material.OXEYE_DAISY, Material.PEONY, Material.POPPY));
        // 1.16+
        bi.put(EntityType.HOGLIN, Collections.singletonList(Material.CRIMSON_FUNGUS));
        bi.put(EntityType.STRIDER, Collections.singletonList(Material.WARPED_FUNGUS));
        // 1.18+
        bi.put(EntityType.AXOLOTL, Collections.singletonList(Material.TROPICAL_FISH_BUCKET));
        bi.put(EntityType.GOAT, Collections.singletonList(Material.WHEAT));
        // 1.19+
        // TODO: remove one 1.18 is dropped.
        if (Enums.getIfPresent(EntityType.class, "FROG").isPresent()) {
            bi.put(EntityType.FROG, Collections.singletonList(Material.SLIME_BALL));
            bi.put(EntityType.ALLAY, Collections.singletonList(Material.AMETHYST_SHARD));
        }
        // Helper
        //  if (Enums.getIfPresent(EntityType.class, "<name>").isPresent()) {
        //      bi.put(EntityType.<type>, Collections.singletonList(Material.<material>));
        //  }
        BREEDING_ITEMS = Collections.unmodifiableMap(bi);
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if (e.getRightClicked() instanceof Animals animal && BREEDING_ITEMS.containsKey(e.getRightClicked().getType())) {
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (e.getHand().equals(EquipmentSlot.OFF_HAND)) {
                inHand = p.getInventory().getItemInOffHand();
            }
            if (BREEDING_ITEMS.get(animal.getType()).contains(inHand.getType()) && !checkIsland(e, p, animal.getLocation(), Flags.BREEDING)) {
                animal.setBreed(false);
            }
        }
    }
}
