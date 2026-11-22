package world.bentobox.bentobox.api.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord.ActionRecords;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests the button types that every templated panel understands (#2531).
 */
class TemplatedPanelTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private TemplatedPanelBuilder builder;

    private final Map<String, BiFunction<ItemTemplateRecord, TemplatedPanel.ItemSlot, PanelItem>> creators = new HashMap<>();
    private PanelTemplateRecord template;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(user.getTranslation(anyString())).thenAnswer(inv -> inv.getArgument(0, String.class));
        when(user.getName()).thenReturn("tastybento");
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.isPlayer()).thenReturn(true);

        template = new PanelTemplateRecord(Panel.Type.INVENTORY, "My menu", null, null, new boolean[6]);
        when(builder.getUser()).thenReturn(user);
        when(builder.getWorld()).thenReturn(world);
        when(builder.getPanelTemplate()).thenReturn(template);
        when(builder.getObjectCreatorMap()).thenReturn(creators);
        when(builder.getParameters()).thenReturn(new ArrayList<>());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private ItemTemplateRecord button(String type, String command, ActionRecords... actions) {
        ItemTemplateRecord rec = new ItemTemplateRecord(new ItemStack(Material.OAK_DOOR), "Back", "Go back", null);
        rec.addData("type", type);
        if (command != null) {
            rec.addData("command", command);
        }
        List.of(actions).forEach(rec::addAction);
        return rec;
    }

    private PanelItem onlyItem() {
        TemplatedPanel panel = new TemplatedPanel(builder);
        assertEquals(1, panel.getItems().size());
        return panel.getItems().values().iterator().next();
    }

    private void click(PanelItem item, ClickType clickType) {
        assertTrue(item.getClickHandler().orElseThrow().onClick(mock(Panel.class), user, clickType, 0));
    }

    @Test
    void testCommandButtonClosesPanelAndRunsCommandAsPlayer() {
        template.addButtonTemplate(0, 0, button("COMMAND", "/menu open main [player]"));
        PanelItem item = onlyItem();
        assertEquals("Back", item.getName());

        click(item, ClickType.LEFT);

        verify(user).closeInventory();
        // Leading slash removed and the player's name filled in
        verify(user).performCommand("menu open main tastybento");
    }

    @Test
    void testCloseButtonOnlyClosesPanel() {
        template.addButtonTemplate(0, 0, button("CLOSE", null));

        click(onlyItem(), ClickType.RIGHT);

        verify(user).closeInventory();
        verify(user, never()).performCommand(anyString());
    }

    @Test
    void testOnlyListedClickTypesWork() {
        template.addButtonTemplate(0, 0,
                button("COMMAND", "island", new ActionRecords(ClickType.LEFT, "run", "", "tips.left-to-run")));
        PanelItem item = onlyItem();
        // Tooltip is shown under the description
        assertTrue(item.getDescription().contains("tips.left-to-run"));

        click(item, ClickType.RIGHT);
        verify(user, never()).performCommand(anyString());

        click(item, ClickType.LEFT);
        verify(user).performCommand("island");
    }

    @Test
    void testCommandButtonWithoutCommandIsLeftOut() {
        template.addButtonTemplate(0, 0, button("COMMAND", "  "));

        TemplatedPanel panel = new TemplatedPanel(builder);

        assertTrue(panel.getItems().isEmpty());
        verify(plugin).logWarning(contains("has no data.command"));
    }

    @Test
    void testPanelsOwnButtonTypeWins() {
        PanelItem own = new PanelItemBuilder().icon(Material.STONE).name("Own button").build();
        creators.put("COMMAND", (rec, slot) -> own);
        template.addButtonTemplate(0, 0, button("COMMAND", "island"));

        PanelItem item = onlyItem();

        assertEquals("Own button", item.getName());
        assertNull(item.getClickHandler().orElse(null));
    }

    @Test
    void testUnknownTypeStillUsesFallback() {
        ItemTemplateRecord fallback = new ItemTemplateRecord(new ItemStack(Material.STONE), "Fallback", null, null);
        ItemTemplateRecord rec = new ItemTemplateRecord(new ItemStack(Material.OAK_DOOR), "Unknown", null, fallback);
        rec.addData("type", "NOT_A_TYPE");
        template.addButtonTemplate(0, 0, rec);

        PanelItem item = onlyItem();

        assertNotNull(item);
        assertEquals("Fallback", item.getName());
    }
}
