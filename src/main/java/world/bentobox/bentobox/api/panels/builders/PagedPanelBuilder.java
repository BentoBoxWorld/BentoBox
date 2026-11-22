package world.bentobox.bentobox.api.panels.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.panels.PagedPanel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;

/**
 * Builds a {@link PagedPanel}: a list of items of any length, shown a page at a time,
 * with previous/next buttons and a bottom row of fixed controls.
 * <p>
 * The bottom row is the control row. Its first and last slots hold the previous and next
 * page buttons when there is a page to go to; the seven slots between them take
 * {@link #control(int, PanelItem) controls} such as Back, Add or Search, which stay put on
 * every page. The rows above hold the {@link #item(PanelItem) items}, in the order added.
 * <p>
 * Example:
 * <pre>{@code
 * PagedPanelBuilder b = new PagedPanelBuilder().user(user).name(user.getTranslation("myaddon.list.title"));
 * things.forEach(t -> b.item(toPanelItem(t)));
 * b.control(4, backButton).build();
 * }</pre>
 *
 * @author tastybento
 * @since 3.24.0
 */
public class PagedPanelBuilder {

    /** Fewest rows a paged panel can have: one of items and the control row */
    public static final int MIN_ROWS = 2;
    /** Most rows an inventory can have */
    public static final int MAX_ROWS = 6;
    /** First control column */
    public static final int FIRST_CONTROL_COLUMN = 1;
    /** Last control column */
    public static final int LAST_CONTROL_COLUMN = 7;

    private String name = "";
    private @Nullable User user;
    private int rows = MAX_ROWS;
    private final List<PanelItem> items = new ArrayList<>();
    private final Map<Integer, PanelItem> controls = new TreeMap<>();
    private @Nullable PanelListener listener;
    private @Nullable Material filler = Material.BLACK_STAINED_GLASS_PANE;
    private int page;

    /**
     * @param name the panel title, which may use MiniMessage or legacy colors; it is the
     *             same on every page
     * @return this builder
     */
    public PagedPanelBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    /**
     * @param user the user to show the panel to; it opens on {@link #build()}
     * @return this builder
     */
    public PagedPanelBuilder user(@NonNull User user) {
        this.user = user;
        return this;
    }

    /**
     * Sets the panel height. Defaults to {@link #MAX_ROWS}.
     *
     * @param rows the number of rows, {@link #MIN_ROWS} to {@link #MAX_ROWS}, including the control row
     * @return this builder
     * @throws IllegalArgumentException if rows is out of range
     */
    public PagedPanelBuilder rows(int rows) {
        if (rows < MIN_ROWS || rows > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "A paged panel needs " + MIN_ROWS + " to " + MAX_ROWS + " rows, not " + rows);
        }
        this.rows = rows;
        return this;
    }

    /**
     * Adds an item to the list.
     *
     * @param item the item
     * @return this builder
     */
    public PagedPanelBuilder item(@NonNull PanelItem item) {
        this.items.add(item);
        return this;
    }

    /**
     * Adds items to the list, in iteration order.
     *
     * @param items the items
     * @return this builder
     */
    public PagedPanelBuilder items(@NonNull Collection<PanelItem> items) {
        this.items.addAll(items);
        return this;
    }

    /**
     * Puts a fixed control in the control row. It shows on every page.
     *
     * @param column the column, {@link #FIRST_CONTROL_COLUMN} to {@link #LAST_CONTROL_COLUMN}
     *               (the end columns hold the page buttons)
     * @param item   the control
     * @return this builder
     * @throws IllegalArgumentException if the column is out of range
     */
    public PagedPanelBuilder control(int column, @NonNull PanelItem item) {
        if (column < FIRST_CONTROL_COLUMN || column > LAST_CONTROL_COLUMN) {
            throw new IllegalArgumentException("Control column must be " + FIRST_CONTROL_COLUMN + " to "
                    + LAST_CONTROL_COLUMN + ", not " + column);
        }
        this.controls.put(column, item);
        return this;
    }

    /**
     * Sets a listener, e.g. to read clicks in the player's own inventory or to act when the
     * panel closes.
     *
     * @param listener the listener
     * @return this builder
     */
    public PagedPanelBuilder listener(@Nullable PanelListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Sets the item that fills the empty slots of the control row. Defaults to black glass panes.
     *
     * @param filler the material, or null to leave the slots empty
     * @return this builder
     */
    public PagedPanelBuilder filler(@Nullable Material filler) {
        this.filler = filler;
        return this;
    }

    /**
     * Sets the page the panel opens on. Out of range values are clamped.
     *
     * @param page the page, starting at 0
     * @return this builder
     */
    public PagedPanelBuilder page(int page) {
        this.page = page;
        return this;
    }

    /**
     * Builds the panel and, if a user was set, opens it at the {@link #page(int) page}.
     *
     * @return the panel
     */
    @NonNull
    public PagedPanel build() {
        return new PagedPanel(this);
    }

    /**
     * @return the number of items shown on each page
     */
    public int getItemsPerPage() {
        return (rows - 1) * 9;
    }

    /**
     * @return the number of pages, at least 1
     */
    public int getPageCount() {
        return Math.max(1, (items.size() + getItemsPerPage() - 1) / getItemsPerPage());
    }

    /**
     * @return the panel title
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * @return the user, or null
     */
    @Nullable
    public User getUser() {
        return user;
    }

    /**
     * @return the number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return the items, in order, unmodifiable
     */
    @NonNull
    public List<PanelItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * @return the controls by column, unmodifiable
     */
    @NonNull
    public Map<Integer, PanelItem> getControls() {
        return Collections.unmodifiableMap(controls);
    }

    /**
     * @return the listener, or null
     */
    @Nullable
    public PanelListener getListener() {
        return listener;
    }

    /**
     * @return the filler material, or null
     */
    @Nullable
    public Material getFiller() {
        return filler;
    }

    /**
     * @return the page to open on, clamped to the pages there are
     */
    public int getPage() {
        return Math.clamp(page, 0, getPageCount() - 1);
    }
}
