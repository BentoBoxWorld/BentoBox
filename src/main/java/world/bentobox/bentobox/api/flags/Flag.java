package world.bentobox.bentobox.api.flags;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.api.flags.clicklisteners.IslandToggleClick;
import world.bentobox.bentobox.api.flags.clicklisteners.WorldToggleClick;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Flag implements Comparable<Flag> {

    /**
     * Defines the behavior and operation of the flag, as well as its category in the {@link world.bentobox.bentobox.panels.SettingsPanel}.
     */
    public enum Type {
        /**
         * Flag protecting an island.
         * It can be modified by the players (island owner).
         * It applies differently depending on the rank of the player who performs the action protected by the flag.
         */
        PROTECTION(Material.SHIELD),
        /**
         * Flag modifying parameters of the island.
         * It can be modified by the players (island owner).
         * This is usually an on/off setting.
         */
        SETTING(Material.COMMAND_BLOCK),
        /**
         * Flag applying to the world.
         * It can only be modified by administrators (permission or operator).
         * This is usually an on/off setting.
         */
        WORLD_SETTING(Material.GRASS_BLOCK);

        private @NonNull Material icon;

        Type(@NonNull Material icon) {
            this.icon = icon;
        }

        @NonNull
        public Material getIcon() {
            return icon;
        }
    }

    private static final String PROTECTION_FLAGS = "protection.flags.";

    private final String id;
    private final Material icon;
    private final Listener listener;
    private final Type type;
    private boolean setting;
    private Map<World, Boolean> defaultWorldSettings = new HashMap<>();
    private final int defaultRank;
    private final PanelItem.ClickHandler clickHandler;
    private final boolean subPanel;
    private Set<GameModeAddon> gameModes = new HashSet<>();

    /**
     * {@link Flag.Builder} should be used instead. This is only used for testing.
     */
    Flag(String id, Material icon, Listener listener, Type type, int defaultRank, PanelItem.ClickHandler clickListener, boolean subPanel, GameModeAddon gameModeAddon) {
        this.id = id;
        this.icon = icon;
        this.listener = listener;
        this.type = type;
        this.defaultRank = defaultRank;
        this.clickHandler = clickListener;
        this.subPanel = subPanel;
        if (gameModeAddon != null) {
            this.gameModes.add(gameModeAddon);
        }
    }

    private Flag(Builder builder) {
        this.id = builder.id;
        this.icon = builder.icon;
        this.listener = builder.listener;
        this.type = builder.type;
        this.setting = builder.defaultSetting;
        this.defaultRank = builder.defaultRank;
        this.clickHandler = builder.clickHandler;
        this.subPanel = builder.usePanel;
        if (builder.gameModeAddon != null) {
            this.gameModes.add(builder.gameModeAddon);
        }
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
     * @return world setting or default flag setting if a specific world setting is not set.
     * If world is not a game world, then the result will always be false!
     */
    public boolean isSetForWorld(World world) {
        if (type.equals(Type.WORLD_SETTING)) {
            WorldSettings ws = BentoBox.getInstance().getIWM().getWorldSettings(world);
            if (ws != null) {
                ws.getWorldFlags().putIfAbsent(getID(), setting);
                return ws.getWorldFlags().get(getID());
            }
            return false;
        } else {
            // Setting
            return defaultWorldSettings.getOrDefault(Util.getWorld(world), setting);
        }
    }

    /**
     * Set a world setting
     * @param world - world
     * @param setting - true or false
     */
    public void setSetting(World world, boolean setting) {
        if (getType().equals(Type.WORLD_SETTING)) {
            BentoBox.getInstance().getIWM().getWorldSettings(world).getWorldFlags().put(getID(), setting);
        }
    }

    /**
     * Set the status of this flag for locations outside of island spaces
     * @param defaultSetting - true means it is allowed. false means it is not allowed
     */
    public void setDefaultSetting(boolean defaultSetting) {
        this.setting = defaultSetting;
    }

    /**
     * Set the status of this flag for locations outside of island spaces for a specific world
     * @param defaultSetting - true means it is allowed. false means it is not allowed
     */
    public void setDefaultSetting(World world, boolean defaultSetting) {
        this.defaultWorldSettings.put(world, defaultSetting);
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

    /**
     * @return whether the flag uses a subpanel or not
     */
    public boolean hasSubPanel() {
        return subPanel;
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
     * @return a locale reference for the name of this protection flag
     */
    public String getNameReference() {
        return PROTECTION_FLAGS + this.id + ".name";
    }

    /**
     * @return a locale reference for the description of this protection flag
     */
    public String getDescriptionReference() {
        return PROTECTION_FLAGS + this.id + ".description";
    }

    /**
     * @return a locale reference for the hint of this protection flag
     */
    public String getHintReference() {
        return PROTECTION_FLAGS + this.id + ".hint";
    }

    /**
     * A set of game mode addons that use this flag. If empty, flag applies to all.
     * @return the gameModeAddon
     */
    public Set<GameModeAddon> getGameModes() {
        return gameModes;
    }

    /**
     * @param gameModeAddon the gameModeAddon to set
     */
    public void setGameModes(Set<GameModeAddon> gameModeAddon) {
        this.gameModes = gameModeAddon;
    }

    /**
     * Add a gameModeAddon to this flag
     * @param gameModeAddon - game mode addon
     */
    public void addGameModeAddon(GameModeAddon gameModeAddon) {
        this.gameModes.add(gameModeAddon);
    }

    /**
     * Remove a gameModeAddon to this flag
     * @param gameModeAddon - game mode addon
     * @return <tt>true</tt> if this set contained the specified element
     */
    public boolean removeGameModeAddon(GameModeAddon gameModeAddon) {
        return this.gameModes.remove(gameModeAddon);
    }

    /**
     * Converts a flag to a panel item. The content of the flag will change depending on who the user is and where they are.
     * @param plugin - plugin
     * @param user - user that will see this flag
     * @param invisible - true if this flag is not visible to players
     * @return - PanelItem for this flag or null if item is inivisible to user
     */
    public PanelItem toPanelItem(BentoBox plugin, User user, boolean invisible) {
        // Invisibility
        if (!user.isOp() && invisible) {
            return null;
        }
        // Start the flag conversion
        PanelItemBuilder pib = new PanelItemBuilder()
                .icon(new ItemStack(icon))
                .name(user.getTranslation("protection.panel.flag-item.name-layout", TextVariables.NAME, user.getTranslation(getNameReference())))
                .clickHandler(clickHandler)
                .invisible(invisible);
        if (hasSubPanel()) {
            pib.description(user.getTranslation("protection.panel.flag-item.menu-layout", TextVariables.DESCRIPTION, user.getTranslation(getDescriptionReference())));
            return pib.build();
        }
        Island island = plugin.getIslands().getIslandAt(user.getLocation()).orElse(plugin.getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        switch(getType()) {
        case PROTECTION:
            return createProtectionFlag(plugin, user, island, pib).build();
        case SETTING:
            return createSettingFlag(user, island, pib).build();
        case WORLD_SETTING:
            return createWorldSettingFlag(user, pib).build();
        default:
            return pib.build();
        }
    }

    private PanelItemBuilder createWorldSettingFlag(User user, PanelItemBuilder pib) {
        String worldSetting = this.isSetForWorld(user.getWorld()) ? user.getTranslation("protection.panel.flag-item.setting-active")
                : user.getTranslation("protection.panel.flag-item.setting-disabled");
        pib.description(user.getTranslation("protection.panel.flag-item.setting-layout", TextVariables.DESCRIPTION, user.getTranslation(getDescriptionReference())
                , "[setting]", worldSetting));
        return pib;
    }

    private PanelItemBuilder createSettingFlag(User user, Island island, PanelItemBuilder pib) {
        if (island != null) {
            String islandSetting = island.isAllowed(this) ? user.getTranslation("protection.panel.flag-item.setting-active")
                    : user.getTranslation("protection.panel.flag-item.setting-disabled");
            pib.description(user.getTranslation("protection.panel.flag-item.setting-layout", TextVariables.DESCRIPTION, user.getTranslation(getDescriptionReference())
                    , "[setting]", islandSetting));
        }
        return pib;
    }

    private PanelItemBuilder createProtectionFlag(BentoBox plugin, User user, Island island, PanelItemBuilder pib) {
        if (island != null) {
            // Protection flag
            pib.description(user.getTranslation("protection.panel.flag-item.description-layout",
                    TextVariables.DESCRIPTION, user.getTranslation(getDescriptionReference())));
            plugin.getRanksManager().getRanks().forEach((reference, score) -> {
                if (score > RanksManager.BANNED_RANK && score < island.getFlag(this)) {
                    pib.description(user.getTranslation("protection.panel.flag-item.blocked-rank") + user.getTranslation(reference));
                } else if (score <= RanksManager.OWNER_RANK && score > island.getFlag(this)) {
                    pib.description(user.getTranslation("protection.panel.flag-item.allowed-rank") + user.getTranslation(reference));
                } else if (score == island.getFlag(this)) {
                    pib.description(user.getTranslation("protection.panel.flag-item.minimal-rank") + user.getTranslation(reference));
                }
            });
        }
        return pib;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Flag [id=" + id + ", icon=" + icon + ", listener=" + listener + ", type=" + type + ", defaultSetting="
                + setting + ", defaultRank=" + defaultRank + ", clickHandler=" + clickHandler + ", subPanel=" + subPanel + "]";
    }

    @Override
    public int compareTo(Flag o) {
        return getID().compareTo(o.getID());
    }

    /**
     * Builder for making flags
     * @author tastybento, Poslovitch
     */
    public static class Builder {
        // Mandatory fields
        private String id;
        private Material icon;

        // Listener
        private Listener listener;

        // Type - is defaulted to PROTECTION
        private Type type = Type.PROTECTION;

        // Default settings
        private boolean defaultSetting = false;
        private int defaultRank = RanksManager.MEMBER_RANK;

        // ClickHandler - default depends on the type
        private PanelItem.ClickHandler clickHandler;

        // Whether there is a sub-panel or not
        private boolean usePanel = false;

        // GameModeAddon
        private GameModeAddon gameModeAddon;

        /**
         * Builder for making flags
         * @param id - a unique id that MUST be the same as the enum of the flag
         * @param icon - a material that will be used as the icon in the GUI
         */
        public Builder(String id, Material icon) {
            this.id = id;
            this.icon = icon;
        }

        /**
         * The listener that should be instantiated to handle events this flag cares about.
         * If the listener class already exists, then do not create it again in another flag.
         * @param listener - Bukkit Listener
         * @return Builder
         */
        public Builder listener(Listener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * The type of flag.
         * @param type {@link Type#PROTECTION}, {@link Type#SETTING} or {@link Type#WORLD_SETTING}
         * @return Builder
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * The click handler to use when this icon is clicked
         * @param clickHandler - click handler
         * @return Builder
         */
        public Builder clickHandler(PanelItem.ClickHandler clickHandler) {
            this.clickHandler = clickHandler;
            return this;
        }

        /**
         * Set the default setting for {@link Type#SETTING} or {@link Type#WORLD_SETTING} flags
         * @param defaultSetting - true or false
         * @return Builder
         */
        public Builder defaultSetting(boolean defaultSetting) {
            this.defaultSetting = defaultSetting;
            return this;
        }

        /**
         * Set the default rank for {@link Type#PROTECTION} flags
         * @param defaultRank - default rank
         * @return Builder
         */
        public Builder defaultRank(int defaultRank) {
            this.defaultRank = defaultRank;
            return this;
        }

        /**
         * Set that this flag icon will open up a sub-panel
         * @param usePanel - true or false
         * @return Builder
         */
        public Builder usePanel(boolean usePanel) {
            this.usePanel = usePanel;
            return this;
        }

        /**
         * Make this flag specific to this gameMode
         * @param gameModeAddon
         * @return
         */
        public Builder setGameMode(GameModeAddon gameModeAddon) {
            this.gameModeAddon = gameModeAddon;
            return this;
        }

        /**
         * Build the flag
         * @return Flag
         */
        public Flag build() {
            // If no clickHandler has been set, then apply default ones
            if (clickHandler == null) {
                switch (type){
                case PROTECTION:
                    clickHandler = new CycleClick(id);
                    break;
                case SETTING:
                    clickHandler = new IslandToggleClick(id);
                    break;
                case WORLD_SETTING:
                    clickHandler = new WorldToggleClick(id);
                    break;
                default:
                    clickHandler = new CycleClick(id);
                    break;
                }
            }

            return new Flag(this);
        }
    }

}
