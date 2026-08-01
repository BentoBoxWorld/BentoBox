package world.bentobox.bentobox.panels.settings;

import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

/**
 * Implements a read-only {@link Tab} that shows the protection flags that apply
 * where the player is standing when they are not on an island, i.e. out in the
 * world. Each flag shows whether it is active or disabled for the world, which
 * is exactly how it is evaluated off-island - can I break blocks out here, can I
 * place them, and so on.
 * <p>
 * This is the player-facing, non-clickable equivalent of
 * {@link WorldDefaultSettingsTab}, which lets admins change the same values.
 *
 * @author tastybento
 * @since 3.22.0
 */
public class WorldProtectionInfoTab extends SettingsTab implements Tab {

    /**
     * @param world - world
     * @param user - user
     */
    public WorldProtectionInfoTab(World world, User user) {
        super(world, user, Type.PROTECTION);
    }

    @Override
    public PanelItem getIcon() {
        return new PanelItemBuilder().icon(Material.STONE_BRICKS).name(getName())
                .description(user.getTranslation(PROTECTION_PANEL + "WORLD_DEFAULTS.description")).build();
    }

    @Override
    public String getName() {
        return user.getTranslation(PROTECTION_PANEL + "WORLD_DEFAULTS.title", "[world_name]",
                plugin.getIWM().getFriendlyName(world));
    }

    /**
     * The flag items are rendered by {@link SettingsTab}, which shows the world's
     * state for each flag because there is no island. They are stripped of their
     * click handlers here: only an admin may change these values, and the flag's
     * own {@link world.bentobox.bentobox.api.flags.clicklisteners.CycleClick}
     * would just tell the player they are not on an island.
     */
    @Override
    public @NonNull List<@Nullable PanelItem> getPanelItems() {
        List<@Nullable PanelItem> items = super.getPanelItems();
        items.stream().filter(Objects::nonNull).forEach(i -> i.setClickHandler(null));
        return items;
    }

}
