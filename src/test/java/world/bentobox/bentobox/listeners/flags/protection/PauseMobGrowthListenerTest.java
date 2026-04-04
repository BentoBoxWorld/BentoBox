package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
class PauseMobGrowthListenerTest extends CommonTestSetup {

    private PauseMobGrowthListener listener;
    private EquipmentSlot hand;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        hand = EquipmentSlot.HAND;

        // Initialize Flags
        Flags.PAUSE_MOB_GROWTH.setDefaultSetting(false);

        listener = new PauseMobGrowthListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Helper to create a mock item with the given material name.
     */
    private void setHeldItem(String materialName) {
        ItemStack mockItem = mock(ItemStack.class);
        Material mockMaterial = mock(Material.class);
        when(mockMaterial.name()).thenReturn(materialName);
        when(mockItem.getType()).thenReturn(mockMaterial);
        when(inv.getItemInMainHand()).thenReturn(mockItem);
    }

    @Test
    void testBabyAnimalWithGoldenDandelionBlocked() {
        setHeldItem("GOLDEN_DANDELION");
        Cow baby = mock(Cow.class);
        when(baby.isAdult()).thenReturn(false);
        when(baby.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, baby, hand);
        listener.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    @Test
    void testBabyAnimalWithGoldenDandelionAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        setHeldItem("GOLDEN_DANDELION");
        Cow baby = mock(Cow.class);
        when(baby.isAdult()).thenReturn(false);
        when(baby.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, baby, hand);
        listener.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    @Test
    void testAdultAnimalWithGoldenDandelionIgnored() {
        setHeldItem("GOLDEN_DANDELION");
        Cow adult = mock(Cow.class);
        when(adult.isAdult()).thenReturn(true);
        when(adult.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, adult, hand);
        listener.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    @Test
    void testBabyAnimalWithOtherItemIgnored() {
        ItemStack mockItem = mock(ItemStack.class);
        when(mockItem.getType()).thenReturn(Material.WHEAT);
        when(inv.getItemInMainHand()).thenReturn(mockItem);
        Cow baby = mock(Cow.class);
        when(baby.isAdult()).thenReturn(false);
        when(baby.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, baby, hand);
        listener.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    @Test
    void testNonAgeableEntityIgnored() {
        setHeldItem("GOLDEN_DANDELION");
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, entity, hand);
        listener.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }
}
