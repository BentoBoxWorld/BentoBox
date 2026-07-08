package world.bentobox.bentobox.api.panels;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.eclipse.jdt.annotation.NonNull;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.heads.HeadGetter;
import world.bentobox.bentobox.util.heads.HeadRequester;

/**
 * A GUI panel that uses the Bukkit inventory API
 * @author tastybento
 *
 */
public class Panel implements HeadRequester, InventoryHolder {

    private Inventory inventory;
    private Map<Integer, PanelItem> items;
    private PanelListener listener;
    private User user;
    private String name;
    /**
     * Cached plain-text rendering of {@link #name}. Computed once whenever the panel is (re)made
     * so that click handling does not have to re-parse the (possibly MiniMessage) title on every
     * click. See {@link #getPlainName()}.
     */
    private String plainName = "";
    private World world;
    private Island island;

    /**
     * Various types of Panels that can be created that use InventoryTypes.
     * <br>
     * The current list of inventories that cannot be created are:<br>
     * <blockquote>
     *     {@link Type#INVENTORY}, {@link Type#HOPPER},
     *     {@link Type#DROPPER}, {@link Type#ANVIL}
     * </blockquote>
     *
     * These relate to the Bukkit inventories with INVENTORY being the standard CHEST inventory.
     * See {@link org.bukkit.event.inventory.InventoryType}.
     * @since 1.7.0
     */
    public enum Type {
        INVENTORY, HOPPER, DROPPER, ANVIL
    }

    public Panel() {
    }

    public Panel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener) {
        this(name, items, size, user, listener, Type.INVENTORY);
    }

    /**
     * @since 1.7.0
     */
    public Panel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener, Type type) {
        makePanel(name, items, size, user, listener, type);
    }

    /**
     * @param pb - PanelBuilder
     * @since 1.16.0
     */
    public Panel(PanelBuilder pb) {
        this.world = pb.getWorld();
        this.makePanel(pb.getName(), pb.getItems(),
                Math.max(pb.getSize(), pb.getItems().isEmpty() ? pb.getSize() : pb.getItems().lastKey() + 1),
                pb.getUser(), pb.getListener(), pb.getPanelType());
    }

    protected void makePanel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener) {
        this.makePanel(name, items, size, user, listener, Type.INVENTORY);
    }

    /**
     * @since 1.7.0
     */
    protected void makePanel(String name, Map<Integer, PanelItem> items, int size, User user, PanelListener listener,
            Type type) {
        this.name = name;
        this.items = items;

        // Create panel with Component-based title
        Component title = name != null ? Util.parseMiniMessageOrLegacy(name) : Component.empty();
        // Cache the plain-text title so click handling does not re-parse it on every click
        this.plainName = Util.componentToPlainText(title);
        switch (type) {
        case INVENTORY -> inventory = Bukkit.createInventory(null, fixSize(size), title);
        case HOPPER -> inventory = Bukkit.createInventory(null, InventoryType.HOPPER, title);
        case DROPPER -> inventory = Bukkit.createInventory(null, InventoryType.DROPPER, title);
        case ANVIL -> inventory = Bukkit.createInventory(null, InventoryType.ANVIL, title);
        }

        // Fill the inventory and return
        for (Map.Entry<Integer, PanelItem> en : items.entrySet()) {
            if (en.getKey() < 54) {
                inventory.setItem(en.getKey(), en.getValue().getItem());
                // Get player head async
                if (en.getValue().isPlayerHead()) {
                    HeadGetter.getHead(en.getValue(), this);
                }
            }
        }
        this.listener = listener;
        // If the listener is defined, then run setup
        if (listener != null)
            listener.setup();

        // If the user is defined, then open panel immediately
        this.user = user;
        if (user != null)
            this.open(user);
    }

    /**
     * Attempts to refresh the panel's contents <b>in place</b>, updating the items of the
     * already-open inventory instead of creating and re-opening a new one.
     * <p>
     * Re-opening an inventory ({@link Bukkit#createInventory} + {@code player.openInventory})
     * fires the full {@code InventoryClose}/{@code InventoryOpen} event cascade across every
     * plugin on the server and resends the entire window to the client. When a panel is only
     * refreshing its contents (e.g. a tabbed panel cycling its display mode or paging) and the
     * title and size are unchanged, that reopen is wasteful — spam-clicking such a panel can
     * measurably raise MSPT. In that case we simply update the item in each slot, which sends
     * only cheap per-slot packets.
     * <p>
     * This is only possible when an inventory already exists (the panel is open), the title
     * ({@link #name}) is unchanged, and the computed size matches the current inventory. If any
     * of those differ (notably a title change, which the client can only pick up on a reopen),
     * the caller must fall back to {@link #makePanel}.
     *
     * @param name  the panel/title name for the refreshed panel
     * @param items the new items keyed by slot
     * @param size  the requested panel size (pre-{@link #fixSize})
     * @return {@code true} if the panel was refreshed in place, {@code false} if the caller must
     *         re-open the panel via {@link #makePanel}
     * @since 3.19.0
     */
    protected boolean tryRefreshInPlace(String name, Map<Integer, PanelItem> items, int size) {
        // Compute the target size from the *incoming* items (matching makePanel, which sets
        // this.items before calling fixSize) so that an auto-sized (size==0) refresh whose new
        // contents need a different inventory size correctly falls back to a full reopen.
        if (inventory == null || !Objects.equals(this.name, name)
                || inventory.getSize() != fixSize(size, items.size())) {
            return false;
        }
        this.items = items;
        int invSize = inventory.getSize();
        for (int i = 0; i < invSize && i < 54; i++) {
            PanelItem pi = items.get(i);
            if (pi == null) {
                inventory.setItem(i, null);
            } else {
                inventory.setItem(i, pi.getItem());
                // Get player head async
                if (pi.isPlayerHead()) {
                    HeadGetter.getHead(pi, this);
                }
            }
        }
        // Mirror makePanel: run listener setup after the contents are updated
        if (listener != null) {
            listener.setup();
        }
        return true;
    }

    private int fixSize(int size) {
        return fixSize(size, items.size());
    }

    private int fixSize(int size, int itemCount) {
        // If size is undefined (0) then use the number of items
        if (size == 0) {
            size = itemCount;
        }
        if (size > 0) {
            // Make sure size is a multiple of 9 and is 54 max.
            size = size + 8;
            size -= (size % 9);
            if (size > 54)
                size = 54;
        } else {
            return 9;
        }
        return size;
    }

    @NonNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, PanelItem> getItems() {
        return items;
    }

    /**
     * @return the listener
     */
    public Optional<PanelListener> getListener() {
        return Optional.ofNullable(listener);
    }

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    public void open(Player... players) {
        for (Player player : players) {
            player.openInventory(inventory);
            PanelListenerManager.getOpenPanels().put(player.getUniqueId(), this);
        }
    }

    /**
     * Open the inventory panel
     * @param users - users that should see the panel
     */
    public void open(User... users) {
        for (User u : users) {
            u.getPlayer().openInventory(inventory);
            PanelListenerManager.getOpenPanels().put(u.getUniqueId(), this);
        }
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * @param items the items to set
     */
    public void setItems(Map<Integer, PanelItem> items) {
        this.items = items;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(PanelListener listener) {
        this.listener = listener;
    }

    /**
     * @param user - the User to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.util.heads.HeadRequester#setHead(world.bentobox.bentobox.api.panels.PanelItem)
     */
    @Override
    public void setHead(PanelItem item) {
        // Update the panel item
        // Find panel item index in items and replace it once more in inventory to update it.
        this.items.entrySet().stream().filter(entry -> entry.getValue() == item).mapToInt(Map.Entry::getKey).findFirst()
                .ifPresent(index ->
                // Update item inside inventory to change icon only if item is inside panel.
                this.inventory.setItem(index, item.getItem()));
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the cached plain-text (colour- and tag-stripped) rendering of the panel's
     * {@link #getName() name}. The panel title may contain MiniMessage tags or legacy {@code §}
     * colour codes; this returns the same plain text that the client shows in the inventory view
     * title. It is computed once when the panel is (re)made rather than on every click, so it is
     * cheap to call from hot paths such as click handling.
     *
     * @return the plain-text panel title, never {@code null} (empty string if the panel has no name)
     * @since 3.19.0
     */
    public String getPlainName() {
        return plainName;
    }

    /**
     * Get the world that applies to this panel
     * @return the optional world
     * @since 1.16.0
     */
    public Optional<World> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * @param world the world to set
     * @since 1.16.0
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * @param island the island to set
     */
    public void setIsland(Island island) {
        this.island = island;
    }

}
