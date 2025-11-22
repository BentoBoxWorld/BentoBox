package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class EntityInteractListenerTest extends CommonTestSetup {

    private EntityInteractListener eil;
    @Mock
    private Entity clickedEntity;
    private Vector position;
    private EquipmentSlot hand;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Clicked block
        when(clickedEntity.getLocation()).thenReturn(location);

        // Hand - main hand
        hand = EquipmentSlot.HAND;
        position = new Vector(10, 10, 10);
        ItemStack mockNameTag = mock(ItemStack.class);
        when(mockNameTag.getType()).thenReturn(Material.NAME_TAG);
        when(inv.getItemInMainHand()).thenReturn(mockNameTag);

        // Initialize the Flags class. This is a workaround to prevent weird errors when mocking
        // I think it's because the flag class needs to be initialized before use in argument matchers
        Flags.TRADING.setDefaultSetting(false);

        // Class under test
        eil = new EntityInteractListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityArmorStandNoInteraction() {
        clickedEntity = mock(ArmorStand.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position, hand);
        eil.onPlayerInteractAtEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityArmorStandAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(ArmorStand.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position, hand);
        eil.onPlayerInteractAtEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityHorseNoInteraction() {
        clickedEntity = mock(Horse.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityHorseAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(Horse.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityMinecartNoInteraction() {
        clickedEntity = mock(RideableMinecart.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityMinecartAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(RideableMinecart.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityBoatNoInteraction() {
        clickedEntity = mock(Boat.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityBoatAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(Boat.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityVillagerNoInteraction() {
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, times(2)).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityVillagerAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test

    public void testOnPlayerInteractEntityNamingVillagerAllowedNoTrading() {
        when(island.isAllowed(any(User.class), eq(Flags.TRADING))).thenReturn(false);
        when(island.isAllowed(any(User.class), eq(Flags.NAME_TAG))).thenReturn(true);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNamingVillagerAllowedTradingNoNaming() {
        when(island.isAllowed(any(User.class), eq(Flags.TRADING))).thenReturn(true);
        when(island.isAllowed(any(User.class), eq(Flags.NAME_TAG))).thenReturn(false);
        clickedEntity = mock(Villager.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityWanderingTraderNoInteraction() {
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        ItemStack mockStone = mock(ItemStack.class);
        when(mockStone.getType()).thenReturn(Material.STONE);
        when(inv.getItemInMainHand()).thenReturn(mockStone);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAtEntityWanderingTraderAllowed() {
        when(island.isAllowed(any(User.class), any())).thenReturn(true);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test

    public void testOnPlayerInteractEntityNamingWanderingTraderAllowedNoTrading() {
        when(island.isAllowed(any(), 
                eq(Flags.TRADING))).thenReturn(false);
        when(island.isAllowed(any(User.class), 
                eq(Flags.NAME_TAG))).thenReturn(true);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test

    public void testOnPlayerInteractEntityNamingWanderingTraderAllowedTradingNoNaming() {
        when(island.isAllowed(any(User.class), eq(Flags.TRADING))).thenReturn(true);
        when(island.isAllowed(any(User.class), eq(Flags.NAME_TAG))).thenReturn(false);
        clickedEntity = mock(WanderingTrader.class);
        when(clickedEntity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(clickedEntity.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntitySheepAllowed() {
        clickedEntity = mock(Sheep.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.SHEEP);
        ItemStack mockAir = mock(ItemStack.class);
        when(mockAir.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(mockAir);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier, never()).notify(any(), eq("protection.protected"));
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntitySheepNameTagNoInteraction() {
        clickedEntity = mock(Sheep.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.SHEEP);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, clickedEntity, hand);
        eil.onPlayerInteractEntity(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }

}
