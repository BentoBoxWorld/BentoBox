package world.bentobox.bentobox.api.flags;

import org.bukkit.Material;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.api.flags.clicklisteners.IslandToggleClick;
import world.bentobox.bentobox.api.flags.clicklisteners.WorldToggleClick;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.managers.RanksManager;

public class FlagBuilder {

    private String id;
    private Material icon;
    private Listener listener;
    private boolean setting;
    private Type type = Type.PROTECTION;
    private int defaultRank = RanksManager.MEMBER_RANK;
    private PanelItem.ClickHandler onClick;
    private boolean subPanel = false;

    public FlagBuilder id(String string) {
        id = string;
        return this;
    }

    /**
     * The material that will become the icon for this flag
     * @param icon - material
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
        // If no onClick has been set, then apply default ones
        if (onClick == null) {
            switch (type){
                case PROTECTION:
                    onClick = new CycleClick(id);
                    break;
                case SETTING:
                    onClick = new IslandToggleClick(id);
                    break;
                case WORLD_SETTING:
                    onClick = new WorldToggleClick(id);
                    break;
                default:
                    onClick = new CycleClick(id);
                    break;
            }
        }

        Flag f = new Flag(id, icon, listener, type, defaultRank, onClick, subPanel);
        f.setDefaultSetting(setting);
        return f;
    }

    /**
     * Sets the default setting for this flag in the world
     * @param setting - true or false
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
     * @param flag - flag
     * @return FlagBuilder
     */
    public FlagBuilder id(Enum<?> flag) {
        id = flag.name();
        return this;
    }
    
    /**
     * Set a default rank for this flag. If not set, the value of RanksManager.MEMBER_RANK will be used
     * @param rank - rank value
     * @return FlagBuilder
     */
    public FlagBuilder defaultRank(int rank) {
        this.defaultRank = rank;
        return this;
    }
    
    /**
     * Adds a listener for clicks on this flag when it is a panel item. Default is
     * {@link world.bentobox.bentobox.api.flags.clicklisteners.CycleClick}
     * @param onClickListener - the listener for clicks. Must use the ClickOn interface
     * @return FlagBuilder
     */
    public FlagBuilder onClick(PanelItem.ClickHandler onClickListener) {
        this.onClick = onClickListener;
        return this;
    }

    /**
     * Marks this flag as "using a sub-panel"
     * @param subPanel - whether the flag will use a sub-panel or not
     * @return FlagBuilder
     */
    public FlagBuilder subPanel(boolean subPanel) {
        this.subPanel = subPanel;
        return this;
    }

}
