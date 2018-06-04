package us.tastybento.bskyblock.api.flags;

import org.bukkit.Material;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.api.flags.Flag.Type;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.listeners.flags.CycleClick;
import us.tastybento.bskyblock.managers.RanksManager;

public class FlagBuilder {

    private String id;
    private Material icon;
    private Listener listener;
    private boolean setting;
    private Type type = Type.PROTECTION;
    private int defaultRank = RanksManager.MEMBER_RANK;
    private PanelItem.ClickHandler onClick;

    public FlagBuilder id(String string) {
        id = string;
        // Set the default click operation to UpDownClick
        onClick = new CycleClick(id);
        return this;
    }

    /**
     * The material that will become the icon for this flag
     * @param icon
     */
    public FlagBuilder icon(Material icon) {
        this.icon = icon;
        return this;
    }

    /**
     * @param listener - the Bukkit listener that will be registered to handle this flag
     */
    public FlagBuilder listener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public Flag build() {
        return new Flag(id, icon, listener, setting, type, defaultRank, onClick);
    }

    /**
     * Sets the default setting for this flag in the world
     * @param setting
     * @return FlagBuilder
     */
    public FlagBuilder allowedByDefault(boolean setting) {
        this.setting = setting;
        return this;
    }

    /**
     * Set the type of this flag
     * @param type {@link Type}
     * @return FlagBuilder
     */
    public FlagBuilder type(Type type) {
        this.type = type;
        return this;
    }

    /**
     * Set the id of this flag to the name of this enum value
     * @param flag
     * @return FlagBuilder
     */
    public FlagBuilder id(Enum<?> flag) {
        id = flag.name();
        return this;
    }
    
    /**
     * Set a default rank for this flag. If not set, the value of RanksManager.MEMBER_RANK will be used
     * @param rank
     * @return FlagBuilder
     */
    public FlagBuilder defaultRank(int rank) {
        this.defaultRank = rank;
        return this;
    }
    
    /**
     * Adds a listener for clicks on this flag when it is a panel item. Default is
     * {@link us.tastybento.bskyblock.listeners.flags.CycleClick}
     * @param onClickListener - the listener for clicks. Must use the ClickOn interface
     * @return FlagBuilder
     */
    public FlagBuilder onClick(PanelItem.ClickHandler onClickListener) {
        this.onClick = onClickListener;
        return this;
    }
    
}
