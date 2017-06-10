package us.tastybento.bskyblock.util.nms.v1_12_R1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.NBTTagString;
import net.minecraft.server.v1_12_R1.TileEntityFlowerPot;
import us.tastybento.bskyblock.util.nms.NMSAbstraction;
import us.tastybento.org.jnbt.CompoundTag;
import us.tastybento.org.jnbt.ListTag;
import us.tastybento.org.jnbt.StringTag;
import us.tastybento.org.jnbt.Tag;

/**
 * NMS Handler for v1_12_R1
 * 
 * @author Tastybento
 * @author Poslovitch
 */
public class NMSHandler implements NMSAbstraction{
    private static HashMap<EntityType, String> bToMConversion;

    static {
        bToMConversion = new HashMap<EntityType, String> ();
        bToMConversion.put(EntityType.MUSHROOM_COW, "mooshroom");
        bToMConversion.put(EntityType.PIG_ZOMBIE, "zombie_pigman");
    }

    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
        net.minecraft.server.v1_12_R1.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_12_R1.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        int combined = blockId + (data << 12);
        IBlockData ibd = net.minecraft.server.v1_12_R1.Block.getByCombinedId(combined);
        if (applyPhysics) {
            w.setTypeAndData(bp, ibd, 3); 
        } else {
            w.setTypeAndData(bp, ibd, 2); 
        }
        chunk.a(bp, ibd);
    }

    @Override
    public ItemStack setBook(Tag item) {
        ItemStack chestItem = new ItemStack(Material.WRITTEN_BOOK);
        //Bukkit.getLogger().info("item data");
        //Bukkit.getLogger().info(item.toString());
        if (((CompoundTag) item).getValue().containsKey("tag")) {
            Map<String,Tag> contents = (Map<String,Tag>) ((CompoundTag) item).getValue().get("tag").getValue();
            //BookMeta bookMeta = (BookMeta) chestItem.getItemMeta();            
            String author = "";
            if (contents.containsKey("author")) {
                author = ((StringTag)contents.get("author")).getValue();
            }
            //Bukkit.getLogger().info("Author: " + author);
            //bookMeta.setAuthor(author);
            String title = "";
            if (contents.containsKey("title")) {
                title = ((StringTag)contents.get("title")).getValue();
            }
            //Bukkit.getLogger().info("Title: " + title);
            //bookMeta.setTitle(title);
            List<String> lore = new ArrayList<String>();
            if (contents.containsKey("display")) {
                Map<String,Tag> display = (Map<String, Tag>) (contents.get("display")).getValue();
                List<Tag> loreTag = ((ListTag)display.get("Lore")).getValue();
                for (Tag s: loreTag) {
                    lore.add(((StringTag)s).getValue());
                }
            }
            //Bukkit.getLogger().info("Lore: " + lore);
            net.minecraft.server.v1_12_R1.ItemStack stack = CraftItemStack.asNMSCopy(chestItem); 
            // Pages
            NBTTagCompound tag = new NBTTagCompound(); //Create the NMS Stack's NBT (item data)
            tag.setString("title", title); //Set the book's title
            tag.setString("author", author);
            if (contents.containsKey("pages")) {
                NBTTagList pages = new NBTTagList();
                List<Tag> pagesTag = ((ListTag)contents.get("pages")).getValue();
                for (Tag s: pagesTag) {
                    pages.add(new NBTTagString(((StringTag)s).getValue()));
                }
                tag.set("pages", pages); //Add the pages to the tag
            }
            stack.setTag(tag); //Apply the tag to the item
            chestItem = CraftItemStack.asCraftMirror(stack); 
            ItemMeta bookMeta = (ItemMeta) chestItem.getItemMeta();
            bookMeta.setLore(lore);
            chestItem.setItemMeta(bookMeta);
        }
        return chestItem;

    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#setBlock(org.bukkit.block.Block, org.bukkit.inventory.ItemStack)
     */
    @Override
    public void setFlowerPotBlock(final Block block, final ItemStack itemStack) {
        if (block.getType().equals(Material.FLOWER_POT)) {
            Location loc = block.getLocation();
            CraftWorld cw = (CraftWorld)block.getWorld();
            BlockPosition bp = new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
            TileEntityFlowerPot te = (TileEntityFlowerPot)cw.getHandle().getTileEntity(bp);
            //Bukkit.getLogger().info("Debug: flowerpot materialdata = " + (new ItemStack(potItem, 1,(short) potItemData).toString()));
            net.minecraft.server.v1_12_R1.ItemStack cis = CraftItemStack.asNMSCopy(itemStack);
            te.setContents(cis);
            te.update();
        }
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#isPotion(org.bukkit.inventory.ItemStack)
     */
    @Override
    public boolean isPotion(ItemStack item) {
        //Bukkit.getLogger().info("DEBUG:item = " + item);
        if (item.getType().equals(Material.POTION)) {
            net.minecraft.server.v1_12_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = stack.getTag();
            //Bukkit.getLogger().info("DEBUG: tag is " + tag);
            //Bukkit.getLogger().info("DEBUG: display is " + tag.getString("display"));
            /*
            for (String list : tag.c()) {
                Bukkit.getLogger().info("DEBUG: list = " + list);
            }*/
            if (tag != null && (!tag.getString("Potion").equalsIgnoreCase("minecraft:water") || tag.getString("Potion").isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.acidisland.nms.NMSAbstraction#setPotion(com.wasteofplastic.org.jnbt.Tag)
     */
    @SuppressWarnings({ "unchecked"})
    @Override
    public ItemStack setPotion(Material material, Tag itemTags, ItemStack chestItem) {
        Map<String,Tag> cont = (Map<String,Tag>) ((CompoundTag) itemTags).getValue();
        if (cont != null) {
            if (((CompoundTag) itemTags).getValue().containsKey("tag")) {
                Map<String,Tag> contents = (Map<String,Tag>)((CompoundTag) itemTags).getValue().get("tag").getValue();
                StringTag stringTag = ((StringTag)contents.get("Potion"));
                if (stringTag != null) {
                    String tag = ((StringTag)contents.get("Potion")).getValue();
                    //Bukkit.getLogger().info("DEBUG: potioninfo found: " + tag);
                    net.minecraft.server.v1_12_R1.ItemStack stack = CraftItemStack.asNMSCopy(chestItem);
                    NBTTagCompound tagCompound = stack.getTag();
                    if(tagCompound == null){
                        tagCompound = new NBTTagCompound();
                    }
                    tagCompound.setString("Potion", tag);
                    stack.setTag(tagCompound);
                    return CraftItemStack.asBukkitCopy(stack);
                }
            }
        }
        // Schematic is old, the potions do not have tags
        // Set it to zero so that the potion bottles don't look like giant purple and black blocks
        chestItem.setDurability((short)0);
        Bukkit.getLogger().warning("Potion in schematic is pre-V1.9 format and will just be water.");
        return chestItem;
    }

    /**
     * Get spawn egg
     * @param type
     * @param amount
     * @return
     */
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        //Bukkit.getLogger().info("DEBUG: setting spawn egg " + type.toString());
        ItemStack item = new ItemStack(Material.MONSTER_EGG, amount);
        net.minecraft.server.v1_12_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tagCompound = stack.getTag();
        if(tagCompound == null){
            tagCompound = new NBTTagCompound();
        }
        //Bukkit.getLogger().info("DEBUG: tag = " + tagCompound);
        NBTTagCompound id = new NBTTagCompound();
        if (!bToMConversion.containsKey(type)) {
            id.setString("id", "minecraft:" + type.toString().toLowerCase());
        } else {
            id.setString("id", "minecraft:" + bToMConversion.get(type));
        }
        tagCompound.set("EntityTag", id);
        stack.setTag(tagCompound);
        //Bukkit.getLogger().info("DEBUG: after tag = " + tagCompound);
        return CraftItemStack.asBukkitCopy(stack);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendTitle(Player player, String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendSubtitle(Player player, String message) {
        // TODO Auto-generated method stub
        
    }
}
