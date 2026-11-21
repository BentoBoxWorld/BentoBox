package world.bentobox.bentobox.api.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.DialogField;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.panels.builders.PagedPanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * A paged panel for choosing one value from a list: a material, an entity type, a biome,
 * or anything else. The chosen value is handed to an {@link #onPick(Consumer) onPick}
 * callback.
 * <p>
 * Ready-made pickers: {@link #materials(User)}, {@link #entities(User)} and
 * {@link #biomes(User)}. For anything else use {@link #of(User, Collection, Function, Function)}.
 * <p>
 * Optional extras:
 * <ul>
 * <li>{@link #fromInventory(Function) Inventory picking}: the player clicks an item in their own
 * inventory to choose it - the quickest way to pick a block. On by default for materials and,
 * with spawn eggs, entities.</li>
 * <li>{@link #search(boolean) Search}: a Search button asks for text in a dialog and narrows the
 * list to names containing it. On by default; hidden where dialogs are not supported.</li>
 * <li>{@link #onCancel(Runnable) Back}: a Back button that runs the given action, e.g. to return to
 * the panel that opened the picker.</li>
 * </ul>
 * Example:
 * <pre>{@code
 * PickerPanel.materials(user)
 *     .title(user.getTranslation("myaddon.pick-block"))
 *     .filter(Material::isBlock)
 *     .onPick(m -> { recipe.setIcon(m); openEditor(user); })
 *     .onCancel(() -> openEditor(user))
 *     .open();
 * }</pre>
 *
 * @param <T> the type of value being chosen
 * @author tastybento
 * @since 3.24.0
 */
public class PickerPanel<T> {

    /** Control column of the Back button */
    static final int BACK_COLUMN = 1;
    /** Control column of the Search button */
    static final int SEARCH_COLUMN = 4;
    /** Control column of the inventory picking hint */
    static final int HINT_COLUMN = 7;

    /** Most characters a search may be */
    private static final int MAX_SEARCH = 32;

    private static final String SPAWN_EGG = "_SPAWN_EGG";
    private static final String SNOW_BLOCK = "SNOW_BLOCK";
    private static final String STONE = "STONE";

    private final User user;
    private final List<T> values;
    private Function<T, ItemStack> icon;
    private Function<T, String> name;
    private Predicate<T> filter = v -> true;
    private @Nullable Consumer<T> onPick;
    private @Nullable Runnable onCancel;
    private @Nullable Function<ItemStack, Optional<T>> fromInventory;
    private boolean search = true;
    private String title = "";
    private String query = "";
    private int rows = PagedPanelBuilder.MAX_ROWS;

    private PickerPanel(User user, Collection<T> values, Function<T, ItemStack> icon, Function<T, String> name) {
        this.user = Objects.requireNonNull(user);
        this.values = List.copyOf(values);
        this.icon = Objects.requireNonNull(icon);
        this.name = Objects.requireNonNull(name);
    }

    // ------------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------------

    /**
     * A picker for any kind of value.
     *
     * @param <T>    the type of value
     * @param user   the user choosing
     * @param values the values to choose from, in the order to show them
     * @param icon   makes the icon for a value
     * @param name   makes the display name of a value; also what {@link #search(boolean) search}
     *               matches against
     * @return the picker
     */
    @NonNull
    public static <T> PickerPanel<T> of(@NonNull User user, @NonNull Collection<T> values,
            @NonNull Function<T, ItemStack> icon, @NonNull Function<T, String> name) {
        return new PickerPanel<>(user, values, icon, name);
    }

    /**
     * A picker for any item material, in the order of the {@link Material} enum. Clicking
     * an item in the inventory chooses its material.
     *
     * @param user the user choosing
     * @return the picker
     */
    @NonNull
    public static PickerPanel<Material> materials(@NonNull User user) {
        List<Material> items = Arrays.stream(Material.values()).filter(PickerPanel::isPickableMaterial).toList();
        return of(user, items, ItemStack::new, m -> Util.prettifyText(m.name()))
                .fromInventory(item -> Optional.of(item.getType()));
    }

    /**
     * A picker for any living entity type that can be spawned, other than the player, sorted
     * by name. Each is shown as its spawn egg where it has one. Clicking a spawn egg in the
     * inventory chooses its entity.
     *
     * @param user the user choosing
     * @return the picker
     */
    @NonNull
    public static PickerPanel<EntityType> entities(@NonNull User user) {
        List<EntityType> types = Arrays.stream(EntityType.values())
                .filter(t -> t != EntityType.PLAYER && t.isSpawnable() && t.isAlive())
                .sorted((a, b) -> a.name().compareTo(b.name())).toList();
        return of(user, types, t -> new ItemStack(spawnEgg(t).orElse(Material.NAME_TAG)),
                t -> Util.prettifyText(t.name())).fromInventory(PickerPanel::entityFromEgg);
    }

    /**
     * A picker for any biome, sorted by name. Each is shown with an icon that suggests it,
     * see {@link #biomeIcon(Biome)}.
     *
     * @param user the user choosing
     * @return the picker
     */
    @NonNull
    public static PickerPanel<Biome> biomes(@NonNull User user) {
        List<Biome> biomes = new ArrayList<>();
        Registry.BIOME.forEach(biomes::add);
        biomes.sort((a, b) -> a.getKey().getKey().compareTo(b.getKey().getKey()));
        return of(user, biomes, b -> new ItemStack(biomeIcon(b)), b -> Util.prettifyText(b.getKey().getKey()));
    }

    // ------------------------------------------------------------------------
    // Options
    // ------------------------------------------------------------------------

    /**
     * @param title the panel title, which may use MiniMessage or legacy colors
     * @return this picker
     */
    public PickerPanel<T> title(@NonNull String title) {
        this.title = title;
        return this;
    }

    /**
     * Narrows the values offered. Values that fail the filter can't be chosen from the
     * inventory either.
     *
     * @param filter which values to offer
     * @return this picker
     */
    public PickerPanel<T> filter(@NonNull Predicate<T> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * @param icon makes the icon for a value, replacing the default
     * @return this picker
     */
    public PickerPanel<T> icon(@NonNull Function<T, ItemStack> icon) {
        this.icon = icon;
        return this;
    }

    /**
     * @param name makes the display name of a value, replacing the default
     * @return this picker
     */
    public PickerPanel<T> name(@NonNull Function<T, String> name) {
        this.name = name;
        return this;
    }

    /**
     * Sets what happens when a value is chosen. The picker closes first, so the callback can
     * open the next panel or dialog.
     *
     * @param onPick the action
     * @return this picker
     */
    public PickerPanel<T> onPick(@NonNull Consumer<T> onPick) {
        this.onPick = onPick;
        return this;
    }

    /**
     * Adds a Back button that runs this action, e.g. to return to the panel that opened
     * the picker.
     *
     * @param onCancel the action, or null for no Back button
     * @return this picker
     */
    public PickerPanel<T> onCancel(@Nullable Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    /**
     * Lets the player choose by clicking an item in their own inventory.
     *
     * @param fromInventory maps a clicked item to a value, or empty if it is not one; null
     *                      turns inventory picking off
     * @return this picker
     */
    public PickerPanel<T> fromInventory(@Nullable Function<ItemStack, Optional<T>> fromInventory) {
        this.fromInventory = fromInventory;
        return this;
    }

    /**
     * @param search whether to show a Search button (only shown where dialogs are supported)
     * @return this picker
     */
    public PickerPanel<T> search(boolean search) {
        this.search = search;
        return this;
    }

    /**
     * @param rows the panel height, including the control row
     * @return this picker
     */
    public PickerPanel<T> rows(int rows) {
        if (rows < PagedPanelBuilder.MIN_ROWS || rows > PagedPanelBuilder.MAX_ROWS) {
            throw new IllegalArgumentException("Rows must be " + PagedPanelBuilder.MIN_ROWS + " to "
                    + PagedPanelBuilder.MAX_ROWS + ", not " + rows);
        }
        this.rows = rows;
        return this;
    }

    // ------------------------------------------------------------------------
    // Showing and picking
    // ------------------------------------------------------------------------

    /**
     * Opens the picker on its first page.
     *
     * @return the panel
     * @throws IllegalStateException if no {@link #onPick(Consumer)} action was set
     */
    @NonNull
    public PagedPanel open() {
        if (onPick == null) {
            throw new IllegalStateException("A picker needs an onPick action");
        }
        PagedPanelBuilder b = new PagedPanelBuilder().user(user).name(title).rows(rows);
        getShown().forEach(v -> b.item(toItem(v)));
        if (onCancel != null) {
            b.control(BACK_COLUMN, control(Material.OAK_DOOR, "panels.picker.back.name",
                    "panels.picker.back.description", () -> {
                        user.closeInventory();
                        onCancel.run();
                    }));
        }
        if (search && Dialogs.isSupported()) {
            b.control(SEARCH_COLUMN, searchButton());
        }
        if (fromInventory != null) {
            b.control(HINT_COLUMN, new PanelItemBuilder().icon(Material.CHEST)
                    .name(user.getTranslation("panels.picker.inventory.name"))
                    .description(user.getTranslation("panels.picker.inventory.description")).build());
            b.listener(new InventoryPickListener());
        }
        return b.build();
    }

    /**
     * @return the values offered now: those passing the filter and, if searching, matching the search
     */
    @NonNull
    public List<T> getShown() {
        String q = query.toLowerCase(Locale.ENGLISH);
        return values.stream().filter(filter)
                .filter(v -> q.isEmpty() || name.apply(v).toLowerCase(Locale.ENGLISH).contains(q)).toList();
    }

    /**
     * @return the current search text, empty if not searching
     */
    @NonNull
    public String getQuery() {
        return query;
    }

    /**
     * Sets the search text. Opening the picker then shows only values whose name contains it.
     *
     * @param query the text, or empty to show everything
     * @return this picker
     */
    public PickerPanel<T> query(@NonNull String query) {
        this.query = query.strip();
        return this;
    }

    /**
     * Chooses a value: closes the picker and runs the {@link #onPick(Consumer) onPick} action.
     * Values the {@link #filter(Predicate) filter} rejects are ignored.
     *
     * @param value the value
     * @return true if the value was accepted
     */
    public boolean pick(@NonNull T value) {
        if (onPick == null || !filter.test(value)) {
            return false;
        }
        user.closeInventory();
        onPick.accept(value);
        return true;
    }

    private PanelItem toItem(T value) {
        return new PanelItemBuilder().icon(icon.apply(value)).name(name.apply(value))
                .description(user.getTranslation("panels.tips.click-to-choose"))
                .clickHandler((panel, u, click, slot) -> {
                    pick(value);
                    return true;
                }).build();
    }

    private PanelItem control(Material material, String nameRef, String descRef, Runnable action) {
        return new PanelItemBuilder().icon(material).name(user.getTranslation(nameRef))
                .description(user.getTranslation(descRef)).clickHandler((panel, u, click, slot) -> {
                    action.run();
                    return true;
                }).build();
    }

    private PanelItem searchButton() {
        if (!query.isEmpty()) {
            // Clicking clears the search
            return control(Material.SPYGLASS, "panels.picker.search.clear-name", "panels.picker.search.clear-description",
                    () -> query("").open());
        }
        return control(Material.SPYGLASS, "panels.picker.search.name", "panels.picker.search.description", () -> {
            user.closeInventory();
            new DialogBuilder().title(user, "panels.picker.search.title")
                    .input(DialogField.text(user, "query", "panels.picker.search.field", query).maxLength(MAX_SEARCH))
                    .button(DialogButton.submit(user, "panels.picker.search.button",
                            (u, response) -> query(response.getText("query").orElse("")).open()))
                    .button(DialogButton.of(user, "panels.picker.back.name", u -> open())).build().show(user);
        });
    }

    /**
     * Turns a click on an item in the player's own inventory into a pick.
     */
    private class InventoryPickListener implements PanelListener {

        @Override
        public void setup() {
            // Nothing to set up
        }

        @Override
        public void onInventoryClose(InventoryCloseEvent event) {
            // Nothing to clean up
        }

        @Override
        public void onInventoryClick(User clicker, InventoryClickEvent event) {
            ItemStack item = event.getCurrentItem();
            if (fromInventory == null || event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getView().getBottomInventory()) || item == null
                    || item.getType().isAir()) {
                return;
            }
            fromInventory.apply(item).ifPresent(PickerPanel.this::pick);
        }
    }

    // ------------------------------------------------------------------------
    // Helpers for the ready-made pickers
    // ------------------------------------------------------------------------

    /**
     * @param m a material
     * @return true if it can be shown as an item
     */
    static boolean isPickableMaterial(Material m) {
        return !m.name().startsWith("LEGACY_") && m.isItem() && !m.isAir();
    }

    /**
     * @param type an entity type
     * @return its spawn egg, if it has one
     */
    @NonNull
    static Optional<Material> spawnEgg(@NonNull EntityType type) {
        return Optional.ofNullable(Material.matchMaterial(type.name() + SPAWN_EGG));
    }

    /**
     * @param item an item
     * @return the entity type of a spawn egg, or empty if it is not one
     */
    @NonNull
    static Optional<EntityType> entityFromEgg(@NonNull ItemStack item) {
        String n = item.getType().name();
        if (!n.endsWith(SPAWN_EGG)) {
            return Optional.empty();
        }
        String type = n.substring(0, n.length() - SPAWN_EGG.length());
        return Arrays.stream(EntityType.values()).filter(t -> t.name().equals(type)).findFirst();
    }

    /** Biome name fragments and the icon each suggests, checked in order */
    private static final List<Map.Entry<String, String>> BIOME_ICONS = List.of(
            Map.entry("crimson", "CRIMSON_NYLIUM"), Map.entry("warped", "WARPED_NYLIUM"),
            Map.entry("soul_sand", "SOUL_SAND"), Map.entry("basalt", "BASALT"), Map.entry("nether", "NETHERRACK"),
            Map.entry("end", "END_STONE"), Map.entry("void", "BLACK_CONCRETE"),
            Map.entry("mushroom", "RED_MUSHROOM_BLOCK"), Map.entry("deep_dark", "SCULK"),
            Map.entry("dripstone", "POINTED_DRIPSTONE"), Map.entry("lush", "MOSS_BLOCK"),
            // Before "grove" and "swamp", which it contains
            Map.entry("mangrove", "MANGROVE_PROPAGULE"),
            Map.entry("frozen", "PACKED_ICE"), Map.entry("ice", "PACKED_ICE"), Map.entry("snow", SNOW_BLOCK),
            Map.entry("grove", SNOW_BLOCK), Map.entry("peak", STONE), Map.entry("slopes", SNOW_BLOCK),
            Map.entry("ocean", "WATER_BUCKET"), Map.entry("river", "WATER_BUCKET"), Map.entry("beach", "SAND"),
            Map.entry("desert", "SAND"), Map.entry("badlands", "RED_SAND"), Map.entry("swamp", "LILY_PAD"), Map.entry("jungle", "JUNGLE_SAPLING"),
            Map.entry("bamboo", "BAMBOO"), Map.entry("cherry", "CHERRY_SAPLING"),
            Map.entry("pale", "PALE_OAK_SAPLING"), Map.entry("dark_forest", "DARK_OAK_SAPLING"),
            Map.entry("birch", "BIRCH_SAPLING"), Map.entry("taiga", "SPRUCE_SAPLING"),
            Map.entry("savanna", "ACACIA_SAPLING"), Map.entry("sunflower", "SUNFLOWER"), Map.entry("flower", "POPPY"),
            Map.entry("forest", "OAK_SAPLING"), Map.entry("meadow", "CORNFLOWER"), Map.entry("hills", STONE),
            Map.entry("stony", STONE), Map.entry("plains", "GRASS_BLOCK"));

    /**
     * Suggests an icon for a biome from its name, e.g. sand for a desert or a lily pad for a
     * swamp, falling back to a grass block.
     *
     * @param biome the biome
     * @return a material for its icon
     */
    @NonNull
    public static Material biomeIcon(@NonNull Biome biome) {
        String key = biome.getKey().getKey();
        return BIOME_ICONS.stream().filter(e -> key.contains(e.getKey()))
                .map(e -> Material.matchMaterial(e.getValue())).filter(Objects::nonNull).findFirst()
                .orElse(Material.GRASS_BLOCK);
    }
}
