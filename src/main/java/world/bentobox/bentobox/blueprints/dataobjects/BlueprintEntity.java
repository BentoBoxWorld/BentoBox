package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.Map;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.util.Transformation;

import com.google.gson.annotations.Expose;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintEntity {

    // Npc storage
    @Expose
    private String npc;

    // MythicMobs storage
    public record MythicMobRecord(String type, String displayName, double level, float power, String stance) {
    }

    // GSON can serialize records, but the record class needs to be know in advance. So this breaks out the record entries
    @Expose
    String MMtype;
    @Expose
    Double MMLevel;
    @Expose
    String MMStance;
    @Expose
    Float MMpower;

    @Expose
    private DyeColor color;
    @Expose
    private EntityType type;
    @Expose
    private String customName;
    @Expose
    private Boolean tamed;
    @Expose
    private Boolean chest;
    @Expose
    private Boolean adult;
    @Expose
    private Integer domestication;
    @Expose
    private Map<Integer, ItemStack> inventory;
    @Expose
    private Style style;
    @Expose
    private Integer level;
    @Expose
    private Profession profession;
    @Expose
    private Integer experience;
    @Expose
    private Villager.Type villagerType;

    /**
     * @since 1.8.0
     */
    public void configureEntity(Entity e) {
        if (e instanceof Villager villager) {
            setVillager(villager);
        }
        if (e instanceof Colorable c) {
            c.setColor(color);
        }
        if (tamed != null && e instanceof Tameable tameable) {
            tameable.setTamed(tamed);
        }
        if (chest != null && e instanceof ChestedHorse chestedHorse) {
            chestedHorse.setCarryingChest(chest);
        }
        if (adult != null && e instanceof Ageable ageable) {
            if (adult) {
                ageable.setAdult();
            } else {
                ageable.setBaby();
            }
        }
        if (e instanceof AbstractHorse horse) {
            if (domestication != null) horse.setDomestication(domestication);
            if (inventory != null) {
                inventory.forEach((index, item) -> horse.getInventory().setItem(index.intValue(), item));

            }
        }
        if (style != null && e instanceof Horse horse) {
            horse.setStyle(style);
        }
    }
    
    /**
     * @param v - villager
     * @since 1.16.0
     */
    private void setVillager(Villager v) {
       v.setProfession(profession == null ? Profession.NONE : profession);
       v.setVillagerExperience(experience == null ? 0 : experience);
       v.setVillagerLevel(level == null ? 0 : level);
       v.setVillagerType(villagerType == null ? Villager.Type.PLAINS : villagerType);
    }
    
    /**
     * @return the color
     */
    public DyeColor getColor() {
        return color;
    }
    /**
     * @param color the color to set
     */
    public void setColor(DyeColor color) {
        this.color = color;
    }
    /**
     * @return the type
     */
    public EntityType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(EntityType type) {
        this.type = type;
    }
    /**
     * @return the customName
     */
    public String getCustomName() {
        return customName;
    }
    /**
     * @param customName the customName to set
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    /**
     * @return the tamed
     */
    public Boolean getTamed() {
        return tamed;
    }
    /**
     * @param tamed the tamed to set
     */
    public void setTamed(Boolean tamed) {
        this.tamed = tamed;
    }
    /**
     * @return the chest
     */
    public Boolean getChest() {
        return chest;
    }
    /**
     * @param chest the chest to set
     */
    public void setChest(Boolean chest) {
        this.chest = chest;
    }
    /**
     * @return the adult
     */
    public Boolean getAdult() {
        return adult;
    }
    /**
     * @param adult the adult to set
     */
    public void setAdult(Boolean adult) {
        this.adult = adult;
    }
    /**
     * @return the domestication
     */
    public Integer getDomestication() {
        return domestication;
    }
    /**
     * @param domestication the domestication to set
     */
    public void setDomestication(int domestication) {
        this.domestication = domestication;
    }
    /**
     * @return the inventory
     */
    public Map<Integer, ItemStack> getInventory() {
        return inventory;
    }
    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Map<Integer, ItemStack> inventory) {
        this.inventory = inventory;
    }
    /**
     * @return the style
     */
    public Style getStyle() {
        return style;
    }
    /**
     * @param style the style to set
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * @return the level
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * @return the profession
     */
    public Profession getProfession() {
        return profession;
    }

    /**
     * @param profession the profession to set
     */
    public void setProfession(Profession profession) {
        this.profession = profession;
    }

    /**
     * @return the experience
     */
    public Integer getExperience() {
        return experience;
    }

    /**
     * @param experience the experience to set
     */
    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    /**
     * @return the villagerType
     */
    public Villager.Type getVillagerType() {
        return villagerType;
    }

    /**
     * @param villagerType the villagerType to set
     */
    public void setVillagerType(Villager.Type villagerType) {
        this.villagerType = villagerType;
    }

    /**
     * @param domestication the domestication to set
     */
    public void setDomestication(Integer domestication) {
        this.domestication = domestication;
    }

    /**
     * @return the mythicMobsRecord
     */
    public MythicMobRecord getMythicMobsRecord() {
        if (this.MMtype == null || this.MMLevel == null || this.MMpower == null || this.MMStance == null) {
            return null;
        }
        return new MythicMobRecord(this.MMtype, this.getCustomName(), this.MMLevel, this.MMpower, this.MMStance);
    }

    /**
     * @param mmr the mythicMobsRecord to set
     * @since 2.1.0
     */
    public void setMythicMobsRecord(MythicMobRecord mmr) {
        this.setCustomName(mmr.displayName());
        this.MMtype = mmr.type();
        this.MMLevel = mmr.level();
        this.MMStance = mmr.stance();
        this.MMpower = mmr.power();
    }

    /**
     * @return the npc
     */
    public String getNpc() {
        return npc;
    }

    /**
     * @param citizen the citizen to set
     */
    public void setNpc(String citizen) {
        this.npc = citizen;
    }

    @Override
    public String toString() {
        return "BlueprintEntity [" + (npc != null ? "npc=" + npc + ", " : "")
                + (MMtype != null ? "MMtype=" + MMtype + ", " : "")
                + (MMLevel != null ? "MMLevel=" + MMLevel + ", " : "")
                + (MMStance != null ? "MMStance=" + MMStance + ", " : "")
                + (MMpower != null ? "MMpower=" + MMpower + ", " : "") + (color != null ? "color=" + color + ", " : "")
                + (type != null ? "type=" + type + ", " : "")
                + (customName != null ? "customName=" + customName + ", " : "")
                + (tamed != null ? "tamed=" + tamed + ", " : "") + (chest != null ? "chest=" + chest + ", " : "")
                + (adult != null ? "adult=" + adult + ", " : "")
                + (domestication != null ? "domestication=" + domestication + ", " : "")
                + (inventory != null ? "inventory=" + inventory + ", " : "")
                + (style != null ? "style=" + style + ", " : "") + (level != null ? "level=" + level + ", " : "")
                + (profession != null ? "profession=" + profession + ", " : "")
                + (experience != null ? "experience=" + experience + ", " : "")
                + (villagerType != null ? "villagerType=" + villagerType : "") + "]";
    }
    



    @Expose
    public DisplayRec displayRec;
    @Expose
    public TextDisplayRec textDisp;
    @Expose
    public BlockData blockDisp;
    @Expose
    public ItemStack itemDisp;

    public record DisplayRec(@Expose Billboard billboard, @Expose Brightness brightness, @Expose float height,
            @Expose float width, @Expose Color glowColorOverride, @Expose int interpolationDelay,
            @Expose int interpolationDuration, @Expose float shadowRadius, @Expose float shadowStrength,
            @Expose int teleportDuration, @Expose Transformation transformation, @Expose float range) {
    }

    public record TextDisplayRec(@Expose String text, @Expose TextAlignment alignment, @Expose Color bgColor,
            @Expose BlockFace face, @Expose int lWidth, @Expose byte opacity, @Expose boolean isShadowed,
            @Expose boolean isSeeThrough, @Expose boolean isDefaultBg) {
    }

    /**
     * BlockDisplay, ItemDisplay, TextDisplay
     * @param disp display entity
     */
    public void storeDisplay(Display disp) {
        // Generic items
        displayRec = new DisplayRec(disp.getBillboard(), disp.getBrightness(), disp.getDisplayHeight(),
                disp.getDisplayWidth(), disp.getGlowColorOverride(), disp.getInterpolationDelay(),
                disp.getInterpolationDuration(), disp.getShadowRadius(), disp.getShadowStrength(),
                disp.getTeleportDuration(), disp.getTransformation(), disp.getViewRange());
        // Class specific items
        if (disp instanceof BlockDisplay bd) {
            this.blockDisp = bd.getBlock();
        } else if (disp instanceof ItemDisplay id) {
            itemDisp = id.getItemStack();
        } else if (disp instanceof TextDisplay td) {
            textDisp = new TextDisplayRec(td.getText(), td.getAlignment(), td.getBackgroundColor(),
                    td.getFacing(), td.getLineWidth(), td.getTextOpacity(), td.isShadowed(), td.isSeeThrough(),
                    td.isDefaultBackground());
        }

        // , getBrightness, getDisplayHeight, getDisplayWidth, getGlowColorOverride, getInterpolationDelay, getInterpolationDuration, 
        //getShadowRadius, getShadowStrength, getTeleportDuration, getTransformation, getViewRange, setBillboard, setBrightness, 
        // setDisplayHeight, setDisplayWidth, setGlowColorOverride, setInterpolationDelay, setInterpolationDuration, setShadowRadius, setShadowStrength, setTeleportDuration, setTransformation, setTransformationMatrix, setViewRange
    }

    /**
     * Sets any display entity properties to the location, e.g. holograms
     * @param pos location
     */
    public void setDisplay(Location pos) {
        World world = pos.getWorld();
        Display d = null;
        if (this.blockDisp != null) {
            // Block Display
            d = world.spawn(pos, BlockDisplay.class);
            ((BlockDisplay) d).setBlock(this.blockDisp);
        } else if (this.itemDisp != null) {
            // Item Display
            d = world.spawn(pos, ItemDisplay.class);
            ((ItemDisplay) d).setItemStack(itemDisp);
        } else if (this.textDisp != null) {
            // Block Display
            d = world.spawn(pos, TextDisplay.class);
            ((TextDisplay) d).setText(textDisp.text());
            ((TextDisplay) d).setAlignment(textDisp.alignment());
            ((TextDisplay) d).setBackgroundColor(textDisp.bgColor());
            ((TextDisplay) d).setLineWidth(textDisp.lWidth());
            ((TextDisplay) d).setTextOpacity(textDisp.opacity());
            ((TextDisplay) d).setShadowed(textDisp.isShadowed());
            ((TextDisplay) d).setSeeThrough(textDisp.isSeeThrough());
            ((TextDisplay) d).setBackgroundColor(textDisp.bgColor());
        }
        if (d != null && this.displayRec != null) {
            d.setBillboard(displayRec.billboard());
            d.setBrightness(displayRec.brightness());
            d.setDisplayHeight(displayRec.height()); 
            d.setDisplayWidth(displayRec.width()); 
            d.setGlowColorOverride(displayRec.glowColorOverride());
            d.setInterpolationDelay(displayRec.interpolationDelay());
            d.setInterpolationDuration(displayRec.interpolationDuration());
            d.setShadowRadius(displayRec.shadowRadius());
            d.setShadowStrength(displayRec.shadowStrength());
            d.setTeleportDuration(displayRec.teleportDuration());
            d.setTransformation(displayRec.transformation());
            d.setViewRange(displayRec.range());
        }
    }

    /**
     * @return the displayRec
     */
    public DisplayRec getDisplayRec() {
        return displayRec;
    }

    /**
     * @param displayRec the displayRec to set
     */
    public void setDisplayRec(DisplayRec displayRec) {
        this.displayRec = displayRec;
    }

    /**
     * @return the blockDisp
     */
    public BlockData getBlockDisp() {
        return blockDisp;
    }

    /**
     * @param blockDisp the blockDisp to set
     */
    public void setBlockDisp(BlockData blockDisp) {
        this.blockDisp = blockDisp;
    }

    /**
     * @return the itemDisp
     */
    public ItemStack getItemDisp() {
        return itemDisp;
    }

    /**
     * @param itemDisp the itemDisp to set
     */
    public void setItemDisp(ItemStack itemDisp) {
        this.itemDisp = itemDisp;
    }

}
