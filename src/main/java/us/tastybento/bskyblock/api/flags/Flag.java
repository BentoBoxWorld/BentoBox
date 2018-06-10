package us.tastybento.bskyblock.api.flags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.RanksManager;
import us.tastybento.bskyblock.util.Util;

public class Flag implements Comparable<Flag> {

    public enum Type {
        PROTECTION,
        SETTING,
        MENU,
        WORLD_SETTING
    }

    private final String id;
    private final Material icon;
    private final Listener listener;
    private final Type type;
    private Map<World, Boolean> worldSettings = new HashMap<>();
    private boolean setting;
    private final int defaultRank;
    private final PanelItem.ClickHandler clickHandler;

    Flag(String id, Material icon, Listener listener, boolean defaultSetting, Type type, int defaultRank, PanelItem.ClickHandler clickListener) {
        this.id = id;
        this.icon = icon;
        this.listener = listener;
        this.setting = defaultSetting;
        this.type = type;
        this.defaultRank = defaultRank;
        this.clickHandler = clickListener;
    }

    public String getID() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public Optional<Listener> getListener() {
        return Optional.ofNullable(listener);
    }

    /**
     * Check if a setting is set in this world
     * @param world - world
     * @return world setting, or default system setting if a specific world setting is not set
     */
    public boolean isSetForWorld(World world) {
        return worldSettings.getOrDefault(Util.getWorld(world), setting);
    }

    /**
     * Set the default or global setting for this world
     * @param world - world
     * @param setting - true or false
     */
    public void setSetting(World world, boolean setting) {
        worldSettings.put(Util.getWorld(world), setting);
    }

    /**
     * Set the status of this flag for locations outside of island spaces
     * @param defaultSetting - true means it is allowed. false means it is not allowed
     */
    public void setDefaultSetting(boolean defaultSetting) {
        this.setting = defaultSetting;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the defaultRank
     */
    public int getDefaultRank() {
        return defaultRank;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Flag)) {
            return false;
        }
        Flag other = (Flag) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }

        return type == other.type;
    }

    /**
     * Converts a flag to a panel item. The content of the flag will change depending on who the user is and where they are.
     * The panel item may reflect their island settings, the island they are on, or the world in general.
     * @param user - user that will see this flag
     * @return - PanelItem for this flag
     */
    public PanelItem toPanelItem(BSkyBlock plugin, User user) {
        // Start the flag conversion
        PanelItemBuilder pib = new PanelItemBuilder()
                .icon(new ItemStack(icon))
                .name(user.getTranslation("protection.panel.flag-item.name-layout", TextVariables.NAME, user.getTranslation("protection.flags." + id + ".name")))
                .clickHandler(clickHandler);
        if (getType().equals(Type.MENU)) {
            pib.description(user.getTranslation("protection.panel.flag-item.menu-layout", "[description]", user.getTranslation("protection.flags." + id + ".description")));
            return pib.build();
        }
        // Check if this is a setting or world setting
        if (getType().equals(Type.WORLD_SETTING)) {
            String setting = this.isSetForWorld(user.getWorld()) ? user.getTranslation("protection.panel.flag-item.setting-active")
                    : user.getTranslation("protection.panel.flag-item.setting-disabled");
            pib.description(user.getTranslation("protection.panel.flag-item.setting-layout", "[description]", user.getTranslation("protection.flags." + id + ".description")
                    , "[setting]", setting));
            return pib.build();
        } 

        // Get the island this user is on or their own
        Island island = plugin.getIslands().getIslandAt(user.getLocation()).orElse(plugin.getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        if (island != null) {
            if (getType().equals(Type.SETTING)) {
                String setting = island.isAllowed(this) ? user.getTranslation("protection.panel.flag-item.setting-active")
                        : user.getTranslation("protection.panel.flag-item.setting-disabled");
                pib.description(user.getTranslation("protection.panel.flag-item.setting-layout", "[description]", user.getTranslation("protection.flags." + id + ".description")
                        , "[setting]", setting));
                return pib.build();
            }
            // TODO: Get the world settings - the player has no island and is not in an island location
            // Dynamic rank list
            if (getType().equals(Type.PROTECTION)) {
                // Protection flag
                pib.description(user.getTranslation("protection.panel.flag-item.description-layout", "[description]", user.getTranslation("protection.flags." + id + ".description")));
                plugin.getRanksManager().getRanks().forEach((reference, score) -> {
                    if (score > RanksManager.BANNED_RANK && score < island.getFlag(this)) {
                        pib.description(user.getTranslation("protection.panel.flag-item.blocked_rank") + user.getTranslation(reference));
                    } else if (score <= RanksManager.OWNER_RANK && score > island.getFlag(this)) {
                        pib.description(user.getTranslation("protection.panel.flag-item.allowed_rank") + user.getTranslation(reference));
                    } else if (score == island.getFlag(this)) {
                        pib.description(user.getTranslation("protection.panel.flag-item.minimal_rank") + user.getTranslation(reference));
                    }
                });
            }
        }
        return pib.build();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Flag [id=" + id + ", icon=" + icon + ", listener=" + listener + ", type=" + type + ", defaultSetting="
                + setting + ", defaultRank=" + defaultRank + ", clickHandler=" + clickHandler + "]";
    }

    @Override
    public int compareTo(Flag o) {
        return getID().compareTo(o.getID());
    }

}
