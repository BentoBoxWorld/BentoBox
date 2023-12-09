package world.bentobox.bentobox.api.panels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class PanelItemTest {

    private PanelItemBuilder pib;
    private PanelItem pi;
    @Mock
    private ClickHandler clickHandler;
    @Mock
    private Server server;
    @Mock
    private ItemFactory itemFac;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");
        when(server.getItemFactory()).thenReturn(itemFac);

        if (Bukkit.getServer() == null) {
            Bukkit.setServer(server);
        }

        pib = new PanelItemBuilder().amount(2).clickHandler(clickHandler).description(List.of("Description", "hello"))
                .icon(new ItemStack(Material.STONE)).name("Name");
        pi = pib.build();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#empty()}.
     */
    @Test
    public void testEmpty() {
        PanelItem panelItem = PanelItem.empty();
        assertTrue(panelItem.getName().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getItem()}.
     */
    @Test
    public void testGetItem() {
        ItemStack i = pi.getItem();
        assertNotNull(i);
        assertEquals(Material.STONE, i.getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getDescription()}.
     */
    @Test
    public void testGetDescription() {
        assertEquals(2, pi.getDescription().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setDescription(java.util.List)}.
     */
    @Test
    public void testSetDescription() {
        assertEquals(2, pi.getDescription().size());
        pi.setDescription(List.of("1", "2", "3"));
        assertEquals(3, pi.getDescription().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getName()}.
     */
    @Test
    public void testGetName() {
        assertEquals("Name", pi.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setName(java.lang.String)}.
     */
    @Test
    public void testSetName() {
        assertEquals("Name", pi.getName());
        pi.setName("Name2");
        assertEquals("Name2", pi.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isInvisible()}.
     */
    @Test
    public void testIsInvisible() {
        assertFalse(pi.isInvisible());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setInvisible(boolean)}.
     */
    @Test
    public void testSetInvisible() {
        assertFalse(pi.isInvisible());
        pi.setInvisible(true);
        assertTrue(pi.isInvisible());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getClickHandler()}.
     */
    @Test
    public void testGetClickHandler() {
        assertEquals(clickHandler, pi.getClickHandler().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setClickHandler(world.bentobox.bentobox.api.panels.PanelItem.ClickHandler)}.
     */
    @Test
    public void testSetClickHandler() {
        assertEquals(clickHandler, pi.getClickHandler().get());
        pi.setClickHandler(null);
        assertEquals(Optional.empty(), pi.getClickHandler());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isGlow()}.
     */
    @Test
    public void testIsGlow() {
        assertFalse(pi.isGlow());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setGlow(boolean)}.
     */
    @Test
    public void testSetGlow() {
        assertFalse(pi.isGlow());
        pi.setGlow(true);
        assertTrue(pi.isGlow());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#isPlayerHead()}.
     */
    @Test
    public void testIsPlayerHead() {
        // Make a head
        pib = new PanelItemBuilder().amount(2).clickHandler(clickHandler).description(List.of("Description", "hello"))
                .icon("tastybento").name("Name");
        pi = pib.build();
        assertTrue(pi.isPlayerHead());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#getPlayerHeadName()}.
     */
    @Test
    public void testGetPlayerHeadName() {
        // Make a head
        pib = new PanelItemBuilder().amount(2).clickHandler(clickHandler).description(List.of("Description", "hello"))
                .icon("tastybento").name("Name");
        pi = pib.build();

        assertEquals("tastybento", pi.getPlayerHeadName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.panels.PanelItem#setHead(org.bukkit.inventory.ItemStack)}.
     */
    @Test
    public void testSetHead() {
        pi.setHead(new ItemStack(Material.PLAYER_HEAD));
    }

}
