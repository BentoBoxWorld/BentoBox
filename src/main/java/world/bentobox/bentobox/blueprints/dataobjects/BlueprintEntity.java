package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;

import com.google.gson.annotations.Expose;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintEntity {

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
                inventory.forEach(horse.getInventory()::setItem);
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
    
}
