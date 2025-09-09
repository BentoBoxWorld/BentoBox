package world.bentobox.bentobox.hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.NPCHook;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * Provides copy and pasting of FancyNPCs in blueprints
 *
 * @author tastybento
 * @since 3.1.0
 */
public class FancyNpcsHook extends NPCHook {

    public FancyNpcsHook() {
        super("FancyNpcs", Material.PLAYER_HEAD);
    }

    String serializeNPC(Npc npc, Vector origin) {
        if (npc == null) {
            throw new IllegalArgumentException("NPC cannot be null.");
        }
        YamlConfiguration npcConfig = new YamlConfiguration();
        NpcData data = npc.getData();
        npcConfig.set("name", data.getName()); // Stored just for reference
        npcConfig.set("creator", data.getCreator().toString());
        npcConfig.set("displayName", data.getDisplayName());
        npcConfig.set("type", data.getType().name());
        npcConfig.set("location.world", data.getLocation().getWorld().getName()); // This will not be used
        // Location is stored relative to the origin, and just stored for reference. x,y,z are not used
        npcConfig.set("location.x", data.getLocation().getX() - origin.getBlockX());
        npcConfig.set("location.y", data.getLocation().getY() - origin.getBlockY());
        npcConfig.set("location.z", data.getLocation().getZ() - origin.getBlockZ());
        // Only yaw and pitch are used
        npcConfig.set("location.yaw", data.getLocation().getYaw());
        npcConfig.set("location.pitch", data.getLocation().getPitch());
        npcConfig.set("showInTab", data.isShowInTab());
        npcConfig.set("spawnEntity", data.isSpawnEntity());
        npcConfig.set("collidable", data.isCollidable());
        npcConfig.set("glowing", data.isGlowing());
        npcConfig.set("glowingColor", data.getGlowingColor().toString());
        npcConfig.set("turnToPlayer", data.isTurnToPlayer());
        npcConfig.set("messages", null);
        npcConfig.set("playerCommands", null);
        npcConfig.set("serverCommands", null);
        npcConfig.set("sendMessagesRandomly", null);
        npcConfig.set("interactionCooldown", data.getInteractionCooldown());
        npcConfig.set("scale", data.getScale());

        if (data.getSkinData() != null) {
            npcConfig.set("skin.identifier", data.getSkinData().getIdentifier());
        } else {
            npcConfig.set("skin.identifier", null);
        }
        npcConfig.set("skin.mirrorSkin", data.isMirrorSkin());

        if (data.getEquipment() != null) {
            for (Entry<NpcEquipmentSlot, org.bukkit.inventory.ItemStack> entry : data.getEquipment().entrySet()) {
                npcConfig.set("equipment." + entry.getKey().name(), entry.getValue());
            }
        }

        for (NpcAttribute attribute : FancyNpcsPlugin.get().getAttributeManager()
                .getAllAttributesForEntityType(data.getType())) {
            String value = data.getAttributes().getOrDefault(attribute, null);
            npcConfig.set("attributes." + attribute.getName(), value);
        }

        npcConfig.set("actions", null);
        for (Map.Entry<ActionTrigger, List<NpcAction.NpcActionData>> entry : npc.getData().getActions().entrySet()) {
            for (NpcAction.NpcActionData actionData : entry.getValue()) {
                if (actionData == null) {
                    continue;
                }

                npcConfig.set("actions." + entry.getKey().name() + "." + actionData.order() + ".action",
                        actionData.action().getName());
                npcConfig.set("actions." + entry.getKey().name() + "." + actionData.order() + ".value",
                        actionData.value());
            }
        }

        return npcConfig.saveToString();
    }

    public boolean spawnNpc(String yaml, Location pos) throws InvalidConfigurationException {
        YamlConfiguration npcConfig = new YamlConfiguration();
        npcConfig.loadFromString(yaml);

        String name = UUID.randomUUID().toString(); // Create a unique name

        UUID creator = UUID.randomUUID(); // Random creator

        String displayName = npcConfig.getString("displayName", "<empty>");
        EntityType type = EntityType.valueOf(npcConfig.getString("type", "PLAYER").toUpperCase(Locale.ENGLISH));

            // Create the spawn location
            Location location;
            double x = pos.getBlockX();
            double y = pos.getBlockY();
            double z = pos.getBlockZ();
            // Add in the yaw and pitch
            float yaw = (float) npcConfig.getDouble("location.yaw");
            float pitch = (float) npcConfig.getDouble("location.pitch");

            location = new Location(pos.getWorld(), x, y, z, yaw, pitch);


            String skinIdentifier = npcConfig.getString("skin.identifier", npcConfig.getString("skin.uuid", ""));

        boolean mirrorSkin = npcConfig.getBoolean("skin.mirrorSkin");

            boolean showInTab = npcConfig.getBoolean("showInTab");
            boolean spawnEntity = npcConfig.getBoolean("spawnEntity");
            boolean collidable = npcConfig.getBoolean("collidable", true);
            boolean glowing = npcConfig.getBoolean("glowing");
            NamedTextColor glowingColor = NamedTextColor.NAMES
                    .value(npcConfig.getString("glowingColor", "white"));
            boolean turnToPlayer = npcConfig.getBoolean("turnToPlayer");

            Map<ActionTrigger, List<NpcAction.NpcActionData>> actions = new ConcurrentHashMap<>();

            ConfigurationSection actiontriggerSection = npcConfig.getConfigurationSection("actions");
            if (actiontriggerSection != null) {
                actiontriggerSection.getKeys(false).forEach(trigger -> {
                    ActionTrigger actionTrigger = ActionTrigger.getByName(trigger);
                    if (actionTrigger == null) {
                        BentoBox.getInstance().logWarning("Could not find action trigger: " + trigger);
                        return;
                    }

                    List<NpcAction.NpcActionData> actionList = new ArrayList<>();
                    ConfigurationSection actionsSection = npcConfig.getConfigurationSection("actions." + trigger);
                    if (actionsSection != null) {
                        actionsSection.getKeys(false).forEach(order -> {
                            String actionName = npcConfig
                                    .getString("actions." + trigger + "." + order + ".action");
                            String value = npcConfig.getString("actions." + trigger + "." + order + ".value");
                            NpcAction action = FancyNpcsPlugin.get().getActionManager().getActionByName(actionName);
                            if (action == null) {
                                BentoBox.getInstance().logWarning("Could not find action: " + actionName);
                                return;
                            }

                            try {
                                actionList.add(new NpcAction.NpcActionData(Integer.parseInt(order), action, value));
                            } catch (NumberFormatException e) {
                                BentoBox.getInstance().logWarning("Could not parse order: " + order);
                            }
                        });

                        actions.put(actionTrigger, actionList);
                    }
                });
            }

            float interactionCooldown = (float) npcConfig.getDouble("interactionCooldown", 0);
            float scale = (float) npcConfig.getDouble("scale", 1);

            Map<NpcAttribute, String> attributes = new HashMap<>();
            if (npcConfig.isConfigurationSection("attributes")) {
                for (String attrName : npcConfig.getConfigurationSection("attributes").getKeys(false)) {
                    NpcAttribute attribute = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(type,
                            attrName);
                    if (attribute == null) {
                        BentoBox.getInstance().logWarning("Could not find attribute: " + attrName);
                        continue;
                    }

                    String value = npcConfig.getString("attributes." + attrName);
                    if (!attribute.isValidValue(value)) {
                        BentoBox.getInstance().logWarning("Invalid value for attribute: " + attrName);
                        continue;
                    }

                    attributes.put(attribute, value);
                }
            }

            FancyNpcsPlugin.get().getNpcManager().getNpc(name);

            // When we make a copy, we need to use a new ID
            String newId = UUID.randomUUID().toString();

            NpcData data = new NpcData(newId, creator, location).setDisplayName(displayName).setSkin(skinIdentifier)
                    .setLocation(location).setShowInTab(showInTab).setSpawnEntity(spawnEntity).setCollidable(collidable)
                    .setGlowing(glowing).setGlowingColor(glowingColor).setType(type).setTurnToPlayer(turnToPlayer)
                    .setActions(actions).setInteractionCooldown(interactionCooldown).setScale(scale)
                    .setMirrorSkin(mirrorSkin);
            attributes.forEach(data::addAttribute);

            Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);

            if (npcConfig.isConfigurationSection("equipment")) {
                for (String equipmentSlotStr : npcConfig.getConfigurationSection("equipment").getKeys(false)) {
                    NpcEquipmentSlot equipmentSlot = NpcEquipmentSlot.parse(equipmentSlotStr);
                    ItemStack item = npcConfig.getItemStack("equipment." + equipmentSlotStr);
                    npc.getData().addEquipment(equipmentSlot, item);
                }
            }

            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
                npc.create();
                npc.spawnForAll();
            });

        return true;
    }

    @Override
    public boolean hook() {
        boolean hooked = this.isPluginAvailable();
        if (!hooked) {
            BentoBox.getInstance().logError("Could not hook into FancyNpcs");
        }
        return hooked; // The hook process shouldn't fail
    }

    @Override
    public String getFailureCause() {
        return null; // The hook process shouldn't fail
    }

    /**
     * Return all NPCs in the chunk
     * @param chunk chunk
     * @return list of NPCs
     */
    public List<Npc> getNPCsInChunk(Chunk chunk) {
        return FancyNpcsPlugin.get().getNpcManager().getAllNpcs().stream()
                .filter(npc -> npc.getData().getLocation().getChunk().equals(chunk)).toList();
    }

    /**
     * Remove all NPCs in chunk
     * @param chunk chunk
     */
    @Override
    public void removeNPCsInChunk(Chunk chunk) {
        getNPCsInChunk(chunk).forEach(Npc::removeForAll);
    }

    @Override
    public Map<? extends Vector, ? extends List<BlueprintEntity>> getNpcsInArea(World world, List<Vector> vectorsToCopy,
            @Nullable Vector origin) {
        Map<Vector, List<BlueprintEntity>> bpEntities = new HashMap<>();
        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            Location npcLocation = npc.getData().getLocation();
            Vector spot = new Vector(npcLocation.getBlockX(), npcLocation.getBlockY(), npcLocation.getBlockZ());
            if (npcLocation.getWorld().equals(world) && vectorsToCopy.contains(spot)) {
                BlueprintEntity cit = new BlueprintEntity();
                cit.setType(npc.getData().getType());
                cit.setNpc(this.serializeNPC(npc, origin));
                // Retrieve or create the list, then add the entity
                List<BlueprintEntity> entities = bpEntities.getOrDefault(spot, new ArrayList<>());
                entities.add(cit);
                // Create position
                Vector origin2 = origin == null ? new Vector(0, 0, 0) : origin;
                int x = spot.getBlockX() - origin2.getBlockX();
                int y = spot.getBlockY() - origin2.getBlockY();
                int z = spot.getBlockZ() - origin2.getBlockZ();
                Vector pos = new Vector(x, y, z);
                // Store
                bpEntities.put(pos, entities); // Update the map
            }
        }
        return bpEntities;
    }

}
