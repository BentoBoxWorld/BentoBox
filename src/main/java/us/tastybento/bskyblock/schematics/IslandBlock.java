package us.tastybento.bskyblock.schematics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import us.tastybento.bskyblock.util.nms.NMSAbstraction;
import us.tastybento.org.jnbt.CompoundTag;
import us.tastybento.org.jnbt.ListTag;
import us.tastybento.org.jnbt.StringTag;
import us.tastybento.org.jnbt.Tag;

public class IslandBlock {
    private short typeId;
    private byte data;
    private int x;
    private int y;
    private int z;
    private List<String> signText;
    private BannerBlock banner;
    private EntityType spawnerBlockType;
    // Chest contents
    private HashMap<Byte,ItemStack> chestContents = new HashMap<>();
    public static final HashMap<String, Material> WEtoM = new HashMap<>();
    public static final HashMap<String, EntityType> WEtoME = new HashMap<>();

    static {
        // Establish the World Edit to Material look up
        // V1.8 items
        if (!Bukkit.getServer().getVersion().contains("(MC: 1.7")) {

            WEtoM.put("ARMORSTAND",Material.ARMOR_STAND);
            WEtoM.put("ACACIA_DOOR",Material.ACACIA_DOOR_ITEM);
            WEtoM.put("BIRCH_DOOR",Material.BIRCH_DOOR_ITEM);
            WEtoM.put("BIRCH_STAIRS",Material.BIRCH_WOOD_STAIRS);
            WEtoM.put("DARK_OAK_DOOR",Material.DARK_OAK_DOOR_ITEM);
            WEtoM.put("JUNGLE_DOOR",Material.JUNGLE_DOOR_ITEM);
            WEtoM.put("SLIME",Material.SLIME_BLOCK);
            WEtoM.put("SPRUCE_DOOR",Material.SPRUCE_DOOR_ITEM);
        }
        WEtoM.put("BREWING_STAND",Material.BREWING_STAND_ITEM);
        WEtoM.put("CARROT_ON_A_STICK",Material.CARROT_STICK);
        WEtoM.put("CARROT",Material.CARROT_ITEM);
        WEtoM.put("CAULDRON", Material.CAULDRON_ITEM);
        WEtoM.put("CLOCK", Material.WATCH);
        WEtoM.put("COBBLESTONE_WALL",Material.COBBLE_WALL);
        WEtoM.put("COMMAND_BLOCK",Material.COMMAND);
        WEtoM.put("COMMANDBLOCK_MINECART",Material.COMMAND_MINECART);
        WEtoM.put("COMPARATOR",Material.REDSTONE_COMPARATOR);
        WEtoM.put("COOKED_PORKCHOP", Material.GRILLED_PORK);
        WEtoM.put("CLOCK", Material.WATCH);
        WEtoM.put("CRAFTING_TABLE", Material.WORKBENCH);
        WEtoM.put("DIAMOND_HORSE_ARMOR",Material.DIAMOND_BARDING);
        WEtoM.put("DIAMOND_SHOVEL",Material.DIAMOND_SPADE);
        WEtoM.put("DYE",Material.INK_SACK);
        WEtoM.put("ENCHANTING_TABLE", Material.ENCHANTMENT_TABLE); //1.11 rename
        WEtoM.put("END_PORTAL_FRAME",Material.ENDER_PORTAL_FRAME);
        WEtoM.put("END_PORTAL", Material.ENDER_PORTAL); // 1.11 rename
        WEtoM.put("END_STONE", Material.ENDER_STONE);
        WEtoM.put("EXPERIENCE_BOTTLE",Material.EXP_BOTTLE);
        WEtoM.put("FILLED_MAP",Material.MAP);
        WEtoM.put("FIRE_CHARGE",Material.FIREBALL);
        WEtoM.put("FIREWORKS",Material.FIREWORK);
        WEtoM.put("FLOWER_POT", Material.FLOWER_POT_ITEM);
        WEtoM.put("GLASS_PANE",Material.THIN_GLASS);
        WEtoM.put("GOLDEN_CHESTPLATE",Material.GOLD_CHESTPLATE);
        WEtoM.put("GOLDEN_HORSE_ARMOR",Material.GOLD_BARDING);
        WEtoM.put("GOLDEN_LEGGINGS",Material.GOLD_LEGGINGS);
        WEtoM.put("GOLDEN_PICKAXE",Material.GOLD_PICKAXE);
        WEtoM.put("GOLDEN_RAIL",Material.POWERED_RAIL);
        WEtoM.put("GOLDEN_SHOVEL",Material.GOLD_SPADE);
        WEtoM.put("GOLDEN_SWORD", Material.GOLD_SWORD);
        WEtoM.put("GOLDEN_HELMET", Material.GOLD_HELMET);
        WEtoM.put("GOLDEN_HOE", Material.GOLD_HOE);
        WEtoM.put("GOLDEN_AXE", Material.GOLD_AXE);
        WEtoM.put("GOLDEN_BOOTS", Material.GOLD_BOOTS);
        WEtoM.put("GUNPOWDER", Material.SULPHUR);
        WEtoM.put("HARDENED_CLAY",Material.HARD_CLAY);
        WEtoM.put("HEAVY_WEIGHTED_PRESSURE_PLATE",Material.GOLD_PLATE);
        WEtoM.put("IRON_BARS",Material.IRON_FENCE);
        WEtoM.put("IRON_HORSE_ARMOR",Material.IRON_BARDING);
        WEtoM.put("IRON_SHOVEL",Material.IRON_SPADE);
        WEtoM.put("LEAD",Material.LEASH);
        WEtoM.put("LEAVES2",Material.LEAVES_2);
        WEtoM.put("LIGHT_WEIGHTED_PRESSURE_PLATE",Material.IRON_PLATE);
        WEtoM.put("LOG2",Material.LOG_2);
        WEtoM.put("MAP",Material.EMPTY_MAP);
        WEtoM.put("MYCELIUM", Material.MYCEL);
        WEtoM.put("NETHER_BRICK_FENCE",Material.NETHER_FENCE);
        WEtoM.put("NETHER_WART",Material.NETHER_STALK);
        WEtoM.put("NETHERBRICK",Material.NETHER_BRICK_ITEM);
        WEtoM.put("OAK_STAIRS",Material.WOOD_STAIRS);
        WEtoM.put("POTATO", Material.POTATO_ITEM);
        WEtoM.put("RAIL",Material.RAILS);
        WEtoM.put("RECORD_11",Material.RECORD_11);
        WEtoM.put("RECORD_13",Material.GOLD_RECORD);
        WEtoM.put("RECORD_BLOCKS",Material.RECORD_3);
        WEtoM.put("RECORD_CAT",Material.GREEN_RECORD);
        WEtoM.put("RECORD_CHIRP",Material.RECORD_4);
        WEtoM.put("RECORD_FAR",Material.RECORD_5);
        WEtoM.put("RECORD_MALL",Material.RECORD_6);
        WEtoM.put("RECORD_MELLOHI",Material.RECORD_7);
        WEtoM.put("RECORD_STAL",Material.RECORD_8);
        WEtoM.put("RECORD_STRAD",Material.RECORD_9);
        WEtoM.put("RECORD_WAIT",Material.RECORD_12);
        WEtoM.put("RECORD_WARD",Material.RECORD_10);
        WEtoM.put("RED_FLOWER",Material.RED_ROSE);
        WEtoM.put("REEDS",Material.SUGAR_CANE);
        WEtoM.put("REPEATER",Material.DIODE);
        WEtoM.put("SKULL", Material.SKULL_ITEM);
        WEtoM.put("SPAWN_EGG",Material.MONSTER_EGG);
        WEtoM.put("STONE_BRICK_STAIRS",Material.BRICK_STAIRS);
        WEtoM.put("STONE_BRICK_STAIRS",Material.SMOOTH_STAIRS);
        WEtoM.put("STONE_SHOVEL",Material.STONE_SPADE);
        WEtoM.put("STONE_SLAB",Material.STEP);
        WEtoM.put("STONE_STAIRS",Material.COBBLESTONE_STAIRS);
        WEtoM.put("TNT_MINECART",Material.EXPLOSIVE_MINECART);
        WEtoM.put("WATERLILY",Material.WATER_LILY);
        WEtoM.put("WHEAT_SEEDS", Material.SEEDS);
        WEtoM.put("WOODEN_AXE",Material.WOOD_AXE);
        WEtoM.put("WOODEN_BUTTON",Material.WOOD_BUTTON);
        WEtoM.put("WOODEN_DOOR",Material.WOOD_DOOR);
        WEtoM.put("WOODEN_HOE",Material.WOOD_HOE);
        WEtoM.put("WOODEN_PICKAXE",Material.WOOD_PICKAXE);
        WEtoM.put("WOODEN_PRESSURE_PLATE",Material.WOOD_PLATE);
        WEtoM.put("WOODEN_SHOVEL",Material.WOOD_SPADE);
        WEtoM.put("WOODEN_SLAB",Material.WOOD_STEP);
        WEtoM.put("WOODEN_SWORD",Material.WOOD_SWORD);
        WEtoM.put("MUSHROOM_STEW",Material.MUSHROOM_SOUP);
        // Entities
        WEtoME.put("LAVASLIME", EntityType.MAGMA_CUBE);
        WEtoME.put("ENTITYHORSE", EntityType.HORSE);
        WEtoME.put("OZELOT", EntityType.OCELOT);
        WEtoME.put("MUSHROOMCOW", EntityType.MUSHROOM_COW);
        WEtoME.put("MOOSHROOM", EntityType.MUSHROOM_COW); // 1.11 rename
        WEtoME.put("PIGZOMBIE", EntityType.PIG_ZOMBIE);
        WEtoME.put("ZOMBIE_PIGMAN", EntityType.PIG_ZOMBIE); // 1.11 rename
        WEtoME.put("CAVESPIDER", EntityType.CAVE_SPIDER);
        WEtoME.put("XPORB", EntityType.EXPERIENCE_ORB);
        WEtoME.put("XP_ORB", EntityType.EXPERIENCE_ORB); // 1.11 rename
        WEtoME.put("MINECARTRIDEABLE", EntityType.MINECART);
        WEtoME.put("MINECARTHOPPER", EntityType.MINECART_HOPPER);
        WEtoME.put("HOPPER_MINECART", EntityType.MINECART_HOPPER);
        WEtoME.put("MINECARTFURNACE", EntityType.MINECART_FURNACE);
        WEtoME.put("FURNACE_MINECART", EntityType.MINECART_FURNACE);
        WEtoME.put("MINECARTMOBSPAWNER", EntityType.MINECART_MOB_SPAWNER);
        WEtoME.put("SPAWNER_MINECART", EntityType.MINECART_MOB_SPAWNER); // 1.11 rename
        WEtoME.put("MINECARTTNT", EntityType.MINECART_TNT);
        WEtoME.put("TNT_MINECART", EntityType.MINECART_TNT); // 1.11
        WEtoME.put("LEASH_KNOT",EntityType.LEASH_HITCH); // 1.11
        WEtoME.put("MINECARTCHEST", EntityType.MINECART_CHEST);
        WEtoME.put("CHEST_MINECART", EntityType.MINECART_CHEST); //1.11 rename
        WEtoME.put("VILLAGERGOLEM", EntityType.IRON_GOLEM);
        WEtoME.put("ENDERDRAGON", EntityType.ENDER_DRAGON);
        WEtoME.put("PAINTING", EntityType.PAINTING);
        WEtoME.put("ITEMFRAME", EntityType.ITEM_FRAME);
        if (!Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
            WEtoME.put("ENDERCRYSTAL", EntityType.ENDER_CRYSTAL);
            WEtoME.put("ARMORSTAND", EntityType.ARMOR_STAND);
        }
        // 1.10 entities and materials
        if (!Bukkit.getServer().getVersion().contains("(MC: 1.7") && !Bukkit.getServer().getVersion().contains("(MC: 1.8") && !Bukkit.getServer().getVersion().contains("(MC: 1.9")) {
            WEtoME.put("POLARBEAR", EntityType.POLAR_BEAR);
            WEtoM.put("ENDER_CRYSTAL", Material.END_CRYSTAL); // 1.11
        }
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public IslandBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        signText = null;
        banner = null;
        spawnerBlockType = null;
        chestContents = new HashMap<>();
    }
    /**
     * @return the type
     */
    public int getTypeId() {
        return typeId;
    }
    /**
     * @param type the type to set
     */
    public void setTypeId(short type) {
        this.typeId = type;
    }
    /**
     * @return the data
     */
    public int getData() {
        return data;
    }
    /**
     * @param data the data to set
     */
    public void setData(byte data) {
        this.data = data;
    }

    /**
     * @return the signText
     */
    public List<String> getSignText() {
        return signText;
    }
    /**
     * @param signText the signText to set
     */
    public void setSignText(List<String> signText) {
        this.signText = signText;
    }

    /**
     * @param s
     * @param b
     */
    public void setBlock(int s, byte b) {
        this.typeId = (short)s;
        this.data = b;
    }

    /**
     * Sets this block up with all the banner data required
     * @param map
     */
    public void setBanner(Map<String, Tag> map) {
        banner = new BannerBlock();
        banner.prep(map);
    }

    /**
     * Sets the spawner type if this block is a spawner
     * @param tileData
     */
    public void setSpawnerType(Map<String, Tag> tileData) {
        //Bukkit.getLogger().info("DEBUG: " + tileData.toString());
        String creatureType = "";
        if (tileData.containsKey("EntityId")) {
            creatureType = ((StringTag) tileData.get("EntityId")).getValue().toUpperCase();
        } else if (tileData.containsKey("SpawnData")) {
            // 1.9 format
            Map<String,Tag> spawnData = ((CompoundTag) tileData.get("SpawnData")).getValue();
            //Bukkit.getLogger().info("DEBUG: " + spawnData.toString());
            if (spawnData.containsKey("id")) {
                creatureType = ((StringTag) spawnData.get("id")).getValue().toUpperCase();
            }
        }
        //Bukkit.getLogger().info("DEBUG: creature type = " + creatureType);
        // The mob type might be prefixed with "Minecraft:"
        if (creatureType.startsWith("MINECRAFT:")) {
            creatureType = creatureType.substring(10);
        }
        if (WEtoME.containsKey(creatureType)) {
            spawnerBlockType = WEtoME.get(creatureType);
        } else {
            try {
                spawnerBlockType = EntityType.valueOf(creatureType);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Spawner setting of " + creatureType + " is unknown for this server. Skipping.");
            }
        }
        //Bukkit.getLogger().info("DEBUG: spawnerblock type = " + spawnerBlockType);
    }

    /**
     * Sets this block's sign data
     * @param tileData
     */
    public void setSign(Map<String, Tag> tileData) {
        signText = new ArrayList<>();
        List<String> text = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            String line = ((StringTag) tileData.get("Text" + String.valueOf(i))).getValue();
            // This value can actually be a string that says null sometimes.
            if (line.equalsIgnoreCase("null")) {
                line = "";
            }
            //System.out.println("DEBUG: line " + i + " = '"+ line + "' of length " + line.length());
            text.add(line);
        }

        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory(){
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }

        };
        // This just removes all the JSON formatting and provides the raw text
        for (int line = 0; line < 4; line++) {
            String lineText = "";
            if (!text.get(line).equals("\"\"") && !text.get(line).isEmpty()) {
                //String lineText = text.get(line).replace("{\"extra\":[\"", "").replace("\"],\"text\":\"\"}", "");
                //Bukkit.getLogger().info("DEBUG: sign text = '" + text.get(line) + "'");
                if (text.get(line).startsWith("{")) {
                    // JSON string
                    try {

                        Map json = (Map)parser.parse(text.get(line), containerFactory);
                        List list = (List) json.get("extra");
                        //System.out.println("DEBUG1:" + JSONValue.toJSONString(list));
                        if (list != null) {
                            Iterator iter = list.iterator();
                            while(iter.hasNext()){
                                Object next = iter.next();
                                String format = JSONValue.toJSONString(next);
                                //System.out.println("DEBUG2:" + format);
                                // This doesn't see right, but appears to be the easiest way to identify this string as JSON...
                                if (format.startsWith("{")) {
                                    // JSON string
                                    Map jsonFormat = (Map)parser.parse(format, containerFactory);
                                    Iterator formatIter = jsonFormat.entrySet().iterator();
                                    while (formatIter.hasNext()) {
                                        Map.Entry entry = (Map.Entry)formatIter.next();
                                        //System.out.println("DEBUG3:" + entry.getKey() + "=>" + entry.getValue());
                                        String key = entry.getKey().toString();
                                        String value = entry.getValue().toString();
                                        if (key.equalsIgnoreCase("color")) {
                                            try {
                                                lineText += ChatColor.valueOf(value.toUpperCase());
                                            } catch (Exception noColor) {
                                                Bukkit.getLogger().warning("Unknown color " + value +" in sign when pasting schematic, skipping...");
                                            }
                                        } else if (key.equalsIgnoreCase("text")) {
                                            lineText += value;
                                        } else {
                                            // Formatting - usually the value is always true, but check just in case
                                            if (key.equalsIgnoreCase("obfuscated") && value.equalsIgnoreCase("true")) {
                                                lineText += ChatColor.MAGIC;
                                            } else if (key.equalsIgnoreCase("underlined") && value.equalsIgnoreCase("true")) {
                                                lineText += ChatColor.UNDERLINE;
                                            } else {
                                                // The rest of the formats
                                                try {
                                                    lineText += ChatColor.valueOf(key.toUpperCase());
                                                } catch (Exception noFormat) {
                                                    // Ignore
                                                    //System.out.println("DEBUG3:" + key + "=>" + value);
                                                    Bukkit.getLogger().warning("Unknown format " + value +" in sign when pasting schematic, skipping...");
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // This is unformatted text. It is included in "". A reset is required to clear
                                    // any previous formatting
                                    if (format.length()>1) {
                                        lineText += ChatColor.RESET + format.substring(format.indexOf('"')+1,format.lastIndexOf('"'));
                                    }
                                }
                            }
                        } else {
                            // No extra tag
                            json = (Map)parser.parse(text.get(line), containerFactory);
                            String value = (String) json.get("text");
                            //System.out.println("DEBUG text only?:" + value);
                            lineText += value;
                        }
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else {
                    // This is unformatted text (not JSON). It is included in "".
                    if (text.get(line).length() > 1) {
                        try {
                            lineText = text.get(line).substring(text.get(line).indexOf('"')+1,text.get(line).lastIndexOf('"'));
                        } catch (Exception e) {
                            //There may not be those "'s, so just use the raw line
                            lineText = text.get(line);
                        }
                    } else {
                        // just in case it isn't - show the raw line
                        lineText = text.get(line);
                    }
                }
                //Bukkit.getLogger().info("Line " + line + " is " + lineText);
            }
            signText.add(lineText);
        }
    }

    public void setBook(Map<String, Tag> tileData) {
        //Bukkit.getLogger().info("DEBUG: Book data ");
        Bukkit.getLogger().info(tileData.toString());
    }

    @SuppressWarnings("deprecation")
    public void setChest(NMSAbstraction nms, Map<String, Tag> tileData) {
        try {
            ListTag chestItems = (ListTag) tileData.get("Items");
            if (chestItems != null) {
                //int number = 0;
                for (Tag item : chestItems.getValue()) {
                    // Format for chest items is:
                    // id = short value of item id
                    // Damage = short value of item damage
                    // Count = the number of items
                    // Slot = the slot in the chest
                    // inventory

                    if (item instanceof CompoundTag) {
                        try {
                            // Id is a number
                            short itemType = (Short) ((CompoundTag) item).getValue().get("id").getValue();
                            short itemDamage = (Short) ((CompoundTag) item).getValue().get("Damage").getValue();
                            byte itemAmount = (Byte) ((CompoundTag) item).getValue().get("Count").getValue();
                            byte itemSlot = (Byte) ((CompoundTag) item).getValue().get("Slot").getValue();
                            ItemStack chestItem = new ItemStack(itemType, itemAmount, itemDamage);
                            chestContents.put(itemSlot, chestItem);
                        } catch (ClassCastException ex) {
                            // Id is a material
                            String itemType = (String) ((CompoundTag) item).getValue().get("id").getValue();
                            try {
                                // Get the material
                                if (itemType.startsWith("minecraft:")) {
                                    String material = itemType.substring(10).toUpperCase();
                                    // Special case for non-standard material names
                                    Material itemMaterial;

                                    //Bukkit.getLogger().info("DEBUG: " + material);

                                    if (WEtoM.containsKey(material)) {
                                        //Bukkit.getLogger().info("DEBUG: Found in hashmap");
                                        itemMaterial = WEtoM.get(material);
                                    } else {
                                        //Bukkit.getLogger().info("DEBUG: Not in hashmap");
                                        itemMaterial = Material.valueOf(material);
                                    }
                                    short itemDamage = (Short) ((CompoundTag) item).getValue().get("Damage").getValue();
                                    byte itemAmount = (Byte) ((CompoundTag) item).getValue().get("Count").getValue();
                                    byte itemSlot = (Byte) ((CompoundTag) item).getValue().get("Slot").getValue();
                                    ItemStack chestItem = new ItemStack(itemMaterial, itemAmount, itemDamage);
                                    if (itemMaterial.equals(Material.WRITTEN_BOOK)) {
                                        chestItem = nms.setBook(item);
                                    }
                                    // Check for potions
                                    if (itemMaterial.toString().contains("POTION")) {
                                        chestItem = nms.setPotion(itemMaterial, item, chestItem);
                                    }
                                    chestContents.put(itemSlot, chestItem);
                                }
                            } catch (Exception exx) {
                                // Bukkit.getLogger().info(item.toString());
                                // Bukkit.getLogger().info(((CompoundTag)item).getValue().get("id").getName());
                                Bukkit.getLogger().severe(
                                        "Could not parse item [" + itemType.substring(10).toUpperCase() + "] in schematic - skipping!");
                                // Bukkit.getLogger().severe(item.toString());
                                exx.printStackTrace();
                            }

                        }

                        // Bukkit.getLogger().info("Set chest inventory slot "
                        // + itemSlot + " to " +
                        // chestItem.toString());
                    }
                }
                //Bukkit.getLogger().info("Added " + number + " items to chest");
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Could not parse schematic file item, skipping!");
            // e.printStackTrace();
        }
    }


    /**
     * Paste this block at blockLoc
     * @param nms
     * @param blockLoc
     */
    //@SuppressWarnings("deprecation")
    @SuppressWarnings("deprecation")
    public void paste(NMSAbstraction nms, Location blockLoc, boolean usePhysics, Biome biome) {
        // Only paste air if it is below the sea level and in the overworld
        Block block = new Location(blockLoc.getWorld(), x, y, z).add(blockLoc).getBlock();
        block.setBiome(biome);
        nms.setBlockSuperFast(block, typeId, data, usePhysics);
        if (signText != null) {
            if (block.getTypeId() != typeId) {
                block.setTypeId(typeId);
            }
            // Sign
            Sign sign = (Sign) block.getState();
            int index = 0;
            for (String line : signText) {
                sign.setLine(index++, line);
            }
            sign.update();
        } else if (banner != null) {
            banner.set(block);
        } else if (spawnerBlockType != null) {
            if (block.getTypeId() != typeId) {
                block.setTypeId(typeId);
            }
            CreatureSpawner cs = (CreatureSpawner)block.getState();
            cs.setSpawnedType(spawnerBlockType);
        } else if (!chestContents.isEmpty()) {
            if (block.getTypeId() != typeId) {
                block.setTypeId(typeId);
            }
            // Check if this is a double chest
            Chest chestBlock = (Chest) block.getState();
            InventoryHolder iH = chestBlock.getInventory().getHolder();
            if (iH instanceof DoubleChest) {
                //Bukkit.getLogger().info("DEBUG: double chest");
                DoubleChest doubleChest = (DoubleChest) iH;
                for (ItemStack chestItem: chestContents.values()) {
                    doubleChest.getInventory().addItem(chestItem);
                }
            } else {
                // Single chest
                for (Entry<Byte, ItemStack> en : chestContents.entrySet()) {
                    chestBlock.getInventory().setItem(en.getKey(), en.getValue());
                }
            }
        }
    }

    /**
     * @return Vector for where this block is in the schematic
     */
    public Vector getVector() {
        return new Vector(x,y,z);
    }
}
