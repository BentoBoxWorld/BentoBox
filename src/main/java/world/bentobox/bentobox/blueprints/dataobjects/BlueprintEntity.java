package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
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
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintEntity {

    // MythicMobs storage
    public record MythicMobRecord(String type, String displayName, double level, float power, String stance) {
    }

    /**
     * Item Display Entity store
     * @since 3.2.0
     */
    public record ItemDispRec(@Expose ItemStack item, @Expose ItemDisplayTransform itemDispTrans) {}

    /**
     * Display Entity store
     * @since 3.2.0
     */
    public record DisplayRec(@Expose Billboard billboard, @Expose Brightness brightness, @Expose float height,
            @Expose float width, @Expose Color glowColorOverride, @Expose int interpolationDelay,
            @Expose int interpolationDuration, @Expose float shadowRadius, @Expose float shadowStrength,
            @Expose int teleportDuration, @Expose Transformation transformation, @Expose float range) {
    }

    /**
     * TextDisplay entity store
     * @since 3.2.0
     */
    public record TextDisplayRec(@Expose String text, @Expose TextAlignment alignment, @Expose Color bgColor,
            @Expose BlockFace face, @Expose int lWidth, @Expose byte opacity, @Expose boolean isShadowed,
            @Expose boolean isSeeThrough, @Expose boolean isDefaultBg) {
    }
    @Expose
    private Boolean adult;
    @Expose
    public BlueprintBlock blockDisp;

    @Expose
    private Boolean chest;
    @Expose
    private DyeColor color;
    @Expose
    private String customName;
    @Expose
    public DisplayRec displayRec;
    @Expose
    private Integer domestication;
    @Expose
    private Integer experience;
    @Expose
    private Map<Integer, ItemStack> inventory;
    @Expose
    public ItemDispRec itemDisp;
    @Expose
    private Integer level;
    @Expose
    Double MMLevel;
    @Expose
    Float MMpower;
    @Expose
    String MMStance;
    // GSON can serialize records, but the record class needs to be know in advance. So this breaks out the record entries
    @Expose
    String MMtype;
    // Npc storage
    @Expose
    private String npc;
    @Expose
    private Profession profession;
    @Expose
    private Style style;

    @Expose
    private Boolean tamed;

    @Expose
    public TextDisplayRec textDisp;

    @Expose
    private EntityType type;
    @Expose
    private Villager.Type villagerType;
    // Position within the block
    @Expose
    private double x;
    @Expose
    private double y;
    @Expose
    private double z;
    @Expose
    private Boolean glowing;
    @Expose
    private Boolean gravity;
    @Expose
    private Boolean visualFire;
    @Expose
    private Boolean silent;
    @Expose
    private Boolean invulnerable;
    @Expose
    private int fireTicks;
    /**
     * Item in ItemFrames
     * @since 3.2.6
     */
    public record ItemFrameRec(
            @Expose
            ItemStack item,
            @Expose
            Rotation rotation,
            @Expose
            boolean isFixed,
            @Expose
            boolean isVisible,
            @Expose
            float dropChance
            ) {}
    /**
     * Item in ItemFrames
     * @since 3.2.6
     */
    @Expose
    private ItemFrameRec itemFrame;
    
    /**
     * Serializes an entity to a Blueprint Entity
     * @param entity entity to serialize
     * @since 3.2.0
     */
    @SuppressWarnings("deprecation")
    public BlueprintEntity(Entity entity) {
        this.setType(entity.getType());
        this.setCustomName(entity.getCustomName());
        this.setGlowing(entity.isGlowing());
        this.setGravity(entity.hasGravity());
        this.setVisualFire(entity.isVisualFire());
        this.setSilent(entity.isSilent());
        this.setInvulnerable(entity.isInvulnerable());
        this.setFireTicks(entity.getFireTicks());

        if (entity instanceof Villager villager) {
            configVillager(villager);
        }
        if (entity instanceof Colorable c && c.getColor() != null) {
            this.setColor(c.getColor());
        }
        if (entity instanceof Tameable tameable) {
            this.setTamed(tameable.isTamed());
        }
        if (entity instanceof ChestedHorse chestedHorse) {
            this.setChest(chestedHorse.isCarryingChest());
        }
        // Only set if child. Most animals are adults
        if (entity instanceof Ageable ageable && !ageable.isAdult()) {
            this.setAdult(false);
        }
        if (entity instanceof AbstractHorse horse) {
            this.setDomestication(horse.getDomestication());
            this.setInventory(new HashMap<>());
            for (int i = 0; i < horse.getInventory().getSize(); i++) {
                ItemStack item = horse.getInventory().getItem(i);
                if (item != null) {
                    this.getInventory().put(i, item);
                }
            }
        }

        if (entity instanceof Horse horse) {
            this.setStyle(horse.getStyle());
        }

        // Display entities
        if (entity instanceof Display disp) {
            this.storeDisplay(disp);
        }
        // ItemFrames
        if (entity instanceof ItemFrame frame) {
            this.setItemFrame(new ItemFrameRec(frame.getItem(), frame.getRotation(), frame.isFixed(), frame.isVisible(), frame.getItemDropChance()));
        }

    }

    /**
     * Makes a blank BlueprintEntity
     */
    public BlueprintEntity() {
        // Blank constructor
    }

    /**
     * Set the villager stats
     * @param v - villager
     * @param bpe - Blueprint Entity
     */
    private void configVillager(Villager v) {
        this.setExperience(v.getVillagerExperience());
        this.setLevel(v.getVillagerLevel());
        this.setProfession(v.getProfession());
        this.setVillagerType(v.getVillagerType());
    }

    /**
     * Adjusts the entity according to how it was stored
     * @since 1.8.0
     */
    public void configureEntity(Entity e) {
        // Set the general states
        e.setGlowing(isGlowing());
        e.setGravity(isGravity());
        e.setVisualFire(isVisualFire());
        e.setSilent(isSilent());
        e.setInvulnerable(isInvulnerable());
        e.setFireTicks(getFireTicks());
        
        if (e instanceof ItemFrame frame) {
            setFrame(frame);
        }

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
        // Shift to the in-block location (remove the 0.5 that the location serializer used)
        e.getLocation().add(new Vector(x - 0.5D, y, z - 0.5D));
    }
    private void setFrame(ItemFrame frame) {
        if (this.itemFrame == null) {
            return;
        }
        frame.setItem(itemFrame.item());
        frame.setVisible(itemFrame.isVisible);
        frame.setFixed(frame.isFixed());
        frame.setRotation(itemFrame.rotation());
        frame.setItemDropChance((float)itemFrame.dropChance()); 
    }

    /**
     * @return the adult
     */
    public Boolean getAdult() {
        return adult;
    }
    /**
     * @return the chest
     */
    public Boolean getChest() {
        return chest;
    }
    /**
     * @return the color
     */
    public DyeColor getColor() {
        return color;
    }
    /**
     * @return the customName
     */
    public String getCustomName() {
        return customName;
    }
    /**
     * @return the domestication
     */
    public Integer getDomestication() {
        return domestication;
    }
    /**
     * @return the experience
     */
    public Integer getExperience() {
        return experience;
    }
    /**
     * @return the inventory
     */
    public Map<Integer, ItemStack> getInventory() {
        return inventory;
    }
    /**
     * @return the level
     */
    public Integer getLevel() {
        return level;
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
     * @return the npc
     */
    public String getNpc() {
        return npc;
    }
    /**
     * @return the profession
     */
    public Profession getProfession() {
        return profession;
    }
    /**
     * @return the style
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @return the tamed
     */
    public Boolean getTamed() {
        return tamed;
    }

    /**
     * @return the type
     */
    public EntityType getType() {
        return type;
    }

    /**
     * @return the villagerType
     */
    public Villager.Type getVillagerType() {
        return villagerType;
    }

    /**
     * @param adult the adult to set
     */
    public void setAdult(Boolean adult) {
        this.adult = adult;
    }

    /**
     * @param chest the chest to set
     */
    public void setChest(Boolean chest) {
        this.chest = chest;
    }

    /**
     * @param color the color to set
     */
    public void setColor(DyeColor color) {
        this.color = color;
    }

    /**
     * @param customName the customName to set
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * Sets any display entity properties to the location, e.g. holograms
     * @param pos location
     */
    public void setDisplay(Location pos) {
        World world = pos.getWorld();
        Location newPos = pos.clone().add(new Vector(x - 0.5D, y, z - 0.5D));
        Display d = null;
        if (this.blockDisp != null) {
            // Block Display
            d = world.spawn(newPos, BlockDisplay.class);
            BlockData bd = Bukkit.createBlockData(this.blockDisp.getBlockData());
            ((BlockDisplay) d).setBlock(bd);
        } else if (this.itemDisp != null) {
            // Item Display
            d = world.spawn(newPos, ItemDisplay.class);
            ((ItemDisplay) d).setItemStack(itemDisp.item());
            ((ItemDisplay) d).setItemDisplayTransform(itemDisp.itemDispTrans());
        } else if (this.textDisp != null) {
            // Text Display
            d = world.spawn(newPos, TextDisplay.class);
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
            d.setCustomName(getCustomName());
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

            // Spawn an armor stand here so that we have a way to detect if a player interacts with the item
            ArmorStand armorStand = (ArmorStand) world.spawnEntity(newPos, EntityType.ARMOR_STAND);
            armorStand.setSmall(true); // Reduces size
            armorStand.setGravity(false); // Prevents falling
            armorStand.setInvisible(true);
            NamespacedKey key = new NamespacedKey(BentoBox.getInstance(), "associatedDisplayEntity");
            armorStand.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.getUniqueId().toString());
        }
    }

    /**
     * @param domestication the domestication to set
     */
    public void setDomestication(int domestication) {
        this.domestication = domestication;
    }

    /**
     * @param domestication the domestication to set
     */
    public void setDomestication(Integer domestication) {
        this.domestication = domestication;
    }

    /**
     * @param experience the experience to set
     */
    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Map<Integer, ItemStack> inventory) {
        this.inventory = inventory;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
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
     * @param npc the citizen to set
     */
    public void setNpc(String npc) {
        this.npc = npc;
    }
    /**
     * @param profession the profession to set
     */
    public void setProfession(Profession profession) {
        this.profession = profession;
    }
    /**
     * @param style the style to set
     */
    public void setStyle(Style style) {
        this.style = style;
    }

    /**
     * @param tamed the tamed to set
     */
    public void setTamed(Boolean tamed) {
        this.tamed = tamed;
    }

    /**
     * @param type the type to set
     */
    public void setType(EntityType type) {
        this.type = type;
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
     * @param villagerType the villagerType to set
     */
    public void setVillagerType(Villager.Type villagerType) {
        this.villagerType = villagerType;
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
            this.blockDisp = new BlueprintBlock(bd.getBlock().getAsString());
        } else if (disp instanceof ItemDisplay id) {
            itemDisp = new ItemDispRec(id.getItemStack(), id.getItemDisplayTransform());
        } else if (disp instanceof TextDisplay td) {
            textDisp = new TextDisplayRec(td.getText(), td.getAlignment(), td.getBackgroundColor(),
                    td.getFacing(), td.getLineWidth(), td.getTextOpacity(), td.isShadowed(), td.isSeeThrough(),
                    td.isDefaultBackground());
        }
        // Store location within block
        x = disp.getLocation().getX() - disp.getLocation().getBlockX();
        y = disp.getLocation().getY() - disp.getLocation().getBlockY();
        z = disp.getLocation().getZ() - disp.getLocation().getBlockZ();
    }

    /**
     * @return the glowing
     */
    public boolean isGlowing() {
        if (glowing == null) {
            glowing = false; // Default
        }
        return glowing;
    }

    /**
     * @param glowing the glowing to set
     */
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    /**
     * @return the gravity
     */
    public boolean isGravity() {
        if (gravity == null) {
            gravity = true;
        }
        return gravity;
    }

    /**
     * @param gravity the gravity to set
     */
    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    /**
     * @return the visualFire
     */
    public boolean isVisualFire() {
        if (visualFire == null) {
            visualFire = false;
        }
        return visualFire;
    }

    /**
     * @param visualFire the visualFire to set
     */
    public void setVisualFire(boolean visualFire) {
        this.visualFire = visualFire;
    }

    /**
     * @return the silent
     */
    public boolean isSilent() {
        if (silent == null) {
            silent = false;
        }
        return silent;
    }

    /**
     * @param silent the silent to set
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * @return the invulnerable
     */
    public boolean isInvulnerable() {
        if (invulnerable == null) {
            invulnerable = false;
        }
        return invulnerable;
    }

    /**
     * @param invulnerable the invulnerable to set
     */
    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    /**
     * @return the fireTicks
     */
    public int getFireTicks() {
        return fireTicks;
    }

    /**
     * @param fireTicks the fireTicks to set
     */
    public void setFireTicks(int fireTicks) {
        this.fireTicks = fireTicks;
    }

    /**
     * @return the itemFrame
     */
    public ItemFrameRec getItemFrame() {
        return itemFrame;
    }

    /**
     * @param itemFrame the itemFrame to set
     */
    public void setItemFrame(ItemFrameRec itemFrame) {
        this.itemFrame = itemFrame;
    }


}
