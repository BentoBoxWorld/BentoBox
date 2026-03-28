package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;
import world.bentobox.bentobox.util.Util;

/**
 * Provides a tab GUI for viewing geo-limited mobs
 * @author tastybento
 *
 */
public class GeoMobLimitTab implements Tab, ClickHandler {

    /**
     * A list of all living entity types, minus some
     */
    private static final List<EntityType> LIVING_ENTITY_TYPES = Arrays.stream(EntityType.values())
            .filter(EntityType::isAlive)
            .filter(t -> !(t.equals(EntityType.PLAYER) || t.equals(EntityType.GIANT) || t.equals(EntityType.ARMOR_STAND)))
            .sorted(Comparator.comparing(EntityType::name))
            .toList();

    /**
     * A list of projectile entity types (for geo-limiting projectiles)
     */
    private static final List<EntityType> PROJECTILE_ENTITY_TYPES = Arrays.stream(EntityType.values())
            .filter(t -> !t.isAlive())
            .filter(t -> {
                Class<?> cls = t.getEntityClass();
                return cls != null && Projectile.class.isAssignableFrom(cls);
            })
            .sorted(Comparator.comparing(EntityType::name))
            .toList();

    /**
     * A combined list of living and projectile entity types for the GEO_LIMIT tab
     */
    private static final List<EntityType> GEO_LIMIT_ENTITY_TYPES = Stream.concat(
            LIVING_ENTITY_TYPES.stream(),
            PROJECTILE_ENTITY_TYPES.stream())
            .sorted(Comparator.comparing(EntityType::name))
            .toList();

    public enum EntityLimitTabType {
        GEO_LIMIT,
        MOB_LIMIT
    }

    private final BentoBox plugin = BentoBox.getInstance();
    private final User user;
    private final EntityLimitTabType type;
    private final World world;
    private TabbedPanel parent;

    /**
     * @param user - user viewing the tab
     * @param type - type of tab to show - Geo limit or Mob limit
     * @param world - world where this tab is being used
     */
    public GeoMobLimitTab(@NonNull User user, @NonNull EntityLimitTabType type, World world) {
        super();
        this.user = user;
        this.type = type;
        this.world = world;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Case panel to Tabbed Panel to get the active page
        TabbedPanel tp = (TabbedPanel)panel;
        // Convert the slot and active page to an index
        int index = tp.getActivePage() * 36 + slot - 9;
        EntityType c = getEntityTypes().get(index);
        if (type == EntityLimitTabType.MOB_LIMIT) {
            if (plugin.getIWM().getMobLimitSettings(world).contains(c.name())) {
                plugin.getIWM().getMobLimitSettings(world).remove(c.name());
            } else {
                plugin.getIWM().getMobLimitSettings(world).add(c.name());
            }
        } else {
            if (plugin.getIWM().getGeoLimitSettings(world).contains(c.name())) {
                plugin.getIWM().getGeoLimitSettings(world).remove(c.name());
            } else {
                plugin.getIWM().getGeoLimitSettings(world).add(c.name());
            }
        }
        // Apply change to panel
        panel.getInventory().setItem(slot, getPanelItem(c, user).getItem());
        // Save settings
        plugin.getIWM().getAddon(Util.getWorld(world)).ifPresent(GameModeAddon::saveWorldSettings);
        return true;
    }

    @Override
    public PanelItem getIcon() {
        if (type == EntityLimitTabType.MOB_LIMIT) {
            return new PanelItemBuilder().icon(Material.IRON_BOOTS).name(getName()).build();
        } else {
            return new PanelItemBuilder().icon(Material.CHAINMAIL_CHESTPLATE).name(getName()).build();

        }
    }

    @Override
    public String getName() {
        return type == EntityLimitTabType.MOB_LIMIT ? user.getTranslation("protection.flags.LIMIT_MOBS.name")
                : user.getTranslation("protection.flags.GEO_LIMIT_MOBS.name");
    }

    @Override
    public List<@Nullable PanelItem> getPanelItems() {
        // Make panel items
        return getEntityTypes().stream().map(c -> getPanelItem(c, user)).toList();
    }

    @Override
    public String getPermission() {
        return "";
    }

    private PanelItem getPanelItem(EntityType c, User user) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(LangUtilsHook.getEntityName(c, user));
        pib.clickHandler(this);
        if (type == EntityLimitTabType.MOB_LIMIT) {
            if (!BentoBox.getInstance().getIWM().getMobLimitSettings(world).contains(c.name())) {
                pib.icon(Material.GREEN_SHULKER_BOX);
                pib.description(user.getTranslation("protection.flags.LIMIT_MOBS.can"));
            } else {
                pib.icon(Material.RED_SHULKER_BOX);
                pib.description(user.getTranslation("protection.flags.LIMIT_MOBS.cannot"));
            }
        } else {
            if (BentoBox.getInstance().getIWM().getGeoLimitSettings(world).contains(c.name())) {
                pib.icon(Material.GREEN_SHULKER_BOX);
                pib.description(user.getTranslation("protection.panel.flag-item.setting-active"));
            } else {
                pib.icon(Material.RED_SHULKER_BOX);
                pib.description(user.getTranslation("protection.panel.flag-item.setting-disabled"));
            }
        }
        return pib.build();
    }

    /**
     * Returns the list of entity types shown in this tab.
     * The GEO_LIMIT tab includes both living entities and projectiles;
     * the MOB_LIMIT tab includes only living entities.
     * @return list of entity types for this tab
     */
    private List<EntityType> getEntityTypes() {
        return type == EntityLimitTabType.GEO_LIMIT ? GEO_LIMIT_ENTITY_TYPES : LIVING_ENTITY_TYPES;
    }

    @Override
    public TabbedPanel getParentPanel() {
        return parent;
    }

    @Override
    public void setParentPanel(TabbedPanel parent) {
        this.parent = parent;
    }

}
