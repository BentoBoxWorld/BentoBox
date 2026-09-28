package world.bentobox.bentobox.panels.customizable;

import java.io.File;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * A templated panel that is its own {@link PanelListener}, so it is redrawn after every click
 * rather than rebuilding itself from its buttons. Clicks are throttled by the click cooldown.
 * Subclasses build the panel, pass it to {@link #setPanel(TemplatedPanel)}, and recompute what it
 * shows in {@link #prepareRefresh()}.
 * @since 3.23.3
 */
public abstract class AbstractRefreshingPanel extends AbstractPanel implements PanelListener {

    @Nullable
    private TemplatedPanel panel;
    private boolean refreshing;
    private boolean closed;

    protected AbstractRefreshingPanel(@NonNull CompositeCommand command, @NonNull User user) {
        super(command, user);
    }

    /**
     * Points the builder at the named template: the game mode's own copy if it has one, else the
     * plugin's.
     * @param panelBuilder the builder
     * @param templateName the template file name, without extension
     */
    protected void selectTemplate(@NonNull TemplatedPanelBuilder panelBuilder, @NonNull String templateName) {
        if (command.getAddon() instanceof GameModeAddon gma && doesCustomPanelExists(gma, templateName)) {
            panelBuilder.template(templateName, new File(gma.getDataFolder(), "panels"));
        } else {
            panelBuilder.template(templateName, new File(plugin.getDataFolder(), "panels"));
        }
    }

    /**
     * Recomputes what the panel shows, just before it is redrawn.
     */
    protected abstract void prepareRefresh();

    /**
     * @return the parameters the panel title is translated with; none by default
     */
    protected String[] titleParameters() {
        return new String[0];
    }

    /**
     * Called once when the panel is truly closed, not when it is reopened by a refresh.
     */
    protected void onClosed() {
        // Nothing by default
    }

    /**
     * Next and previous buttons call this; the panel is refreshed by {@link #refreshPanel()} after
     * every click anyway, so there is nothing to do here.
     */
    @Override
    protected void build() {
        // Refreshed by the listener after the click
    }

    @Override
    protected void onPageChanged() {
        // Refreshed by the listener after the click
    }

    @Override
    public void setup() {
        // Nothing to set up
    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Clicks are handled by the buttons
    }

    @Override
    public void refreshPanel() {
        if (closed || panel == null) {
            return;
        }
        prepareRefresh();
        // Mark as refreshing so that the inventory close fired by a reopen is not treated as a
        // true close
        refreshing = true;
        try {
            panel.regenerate(titleParameters());
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!refreshing) {
            closed = true;
            onClosed();
        }
    }

    @Override
    public boolean hasClickCooldown() {
        return true;
    }

    @Override
    public boolean isActionableSlot(int rawSlot) {
        if (panel == null) {
            return false;
        }
        PanelItem item = panel.getItems().get(rawSlot);
        return item != null && item.getClickHandler().isPresent();
    }

    /**
     * @param panel the panel that was opened
     */
    protected void setPanel(@Nullable TemplatedPanel panel) {
        this.panel = panel;
    }

    /**
     * @return the panel that is open, or null if none
     */
    @Nullable
    public TemplatedPanel getPanel() {
        return panel;
    }
}
