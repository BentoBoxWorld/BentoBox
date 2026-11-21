package world.bentobox.bentobox.api.panels;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * A panel that shows a list of any length a page at a time. Build one with a
 * {@link PagedPanelBuilder}.
 * <p>
 * Turning the page refreshes the open inventory in place rather than re-opening it, so
 * paging through a long list is cheap and the cursor stays where it was.
 *
 * @author tastybento
 * @since 3.24.0
 */
public class PagedPanel extends Panel {

    private final PagedPanelBuilder builder;
    private int page;

    /**
     * Makes the panel and, if the builder has a user, opens it.
     *
     * @param builder the builder
     */
    public PagedPanel(@NonNull PagedPanelBuilder builder) {
        this.builder = builder;
        this.page = builder.getPage();
        makePanel(builder.getName(), itemsFor(page), builder.getRows() * 9, builder.getUser(), builder.getListener());
    }

    /**
     * Shows another page. Out of range values are clamped.
     *
     * @param page the page, starting at 0
     */
    public void showPage(int page) {
        this.page = Math.clamp(page, 0, builder.getPageCount() - 1);
        Map<Integer, PanelItem> items = itemsFor(this.page);
        if (!tryRefreshInPlace(builder.getName(), items, builder.getRows() * 9)) {
            makePanel(builder.getName(), items, builder.getRows() * 9, builder.getUser(), builder.getListener());
        }
    }

    /**
     * @return the page showing, starting at 0
     */
    public int getPage() {
        return page;
    }

    /**
     * @return the number of pages, at least 1
     */
    public int getPageCount() {
        return builder.getPageCount();
    }

    /**
     * Works out which item goes in which slot for a page.
     *
     * @param page the page
     * @return the items by slot
     */
    @NonNull
    Map<Integer, PanelItem> itemsFor(int page) {
        Map<Integer, PanelItem> slots = new TreeMap<>();
        int perPage = builder.getItemsPerPage();
        List<PanelItem> all = builder.getItems();
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < all.size(); i++) {
            slots.put(i, all.get(start + i));
        }
        int controlRow = perPage; // first slot of the last row
        User user = builder.getUser();
        if (page > 0) {
            slots.put(controlRow, pageButton(user, "panels.buttons.previous", Material.ARROW, page - 1));
        }
        if (page < builder.getPageCount() - 1) {
            slots.put(controlRow + 8, pageButton(user, "panels.buttons.next", Material.ARROW, page + 1));
        }
        builder.getControls().forEach((column, item) -> slots.put(controlRow + column, item));
        Material filler = builder.getFiller();
        if (filler != null) {
            PanelItem blank = new PanelItemBuilder().icon(filler).name(" ").build();
            for (int column = 0; column < 9; column++) {
                slots.putIfAbsent(controlRow + column, blank);
            }
        }
        return slots;
    }

    private PanelItem pageButton(User user, String reference, Material icon, int target) {
        // Pages are shown to the player counting from 1
        String number = String.valueOf(target + 1);
        PanelItemBuilder b = new PanelItemBuilder().icon(icon).amount(Math.clamp(target + 1, 1, 64))
                .clickHandler((panel, u, clickType, slot) -> {
                    showPage(target);
                    return true;
                });
        if (user != null) {
            b.name(user.getTranslation(reference + ".name"))
                    .description(user.getTranslation(reference + ".description", TextVariables.NUMBER, number));
        } else {
            b.name(number);
        }
        return b.build();
    }
}
