package world.bentobox.bentobox.hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import lol.pyr.znpcsplus.ZNPCsPlus;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.interaction.InteractionAction;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.npc.NpcType;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * Provides copy and pasting of ZNPCS Plus in blueprints https://github.com/Pyrbu/ZNPCsPlus
 *
 * @author tastybento
 * @since 3.2.0
 */
public class ZNPCSPlusHook extends Hook {

    private ZNPCsPlus plugin;

    public ZNPCSPlusHook() {
        super("ZNPCsPlus", Material.PLAYER_HEAD);

    }

    public String serializeNPC(Npc npc, Vector origin) {
        if (npc == null) {
            throw new IllegalArgumentException("NPC cannot be null.");
        }
        YamlConfiguration config = new YamlConfiguration();
        NpcType type = npc.getType();
        for (EntityProperty<?> property : npc.getAppliedProperties())
            try {
                PropertySerializer<?> serializer = propertyRegistry
                        .getSerializer(((EntityPropertyImpl<?>) property).getType());
                if (serializer == null) {
                    BentoBox.getInstance().logWarning("Unknown serializer for property '" + property.getName()
                            + "' for npc '" + npc.getUuid() + "'. skipping ...");
                    continue;
            }
                config.set("properties." + property.getName(), serializer.UNSAFE_serialize(npc.getProperty(property)));
            } catch (Exception exception) {
                BentoBox.getInstance().logWarning(
                        "Failed to serialize property " + property.getName() + " for npc with id " + npc.getUuid());
                exception.printStackTrace();
        }

        lol.pyr.znpcsplus.api.hologram.Hologram hologram = npc.getHologram();
        if (hologram.getRefreshDelay() != -1)
            config.set("hologram.refresh-delay", hologram.getRefreshDelay());
        List<String> lines = new ArrayList<>(npc.getHologram().lineCount());
        for (int i = 0; i < npc.getHologram().lineCount(); i++) {
            lines.add(hologram.getLine(i));
        }
        config.set("hologram.lines", lines);
        config.set("actions", npc.getActions().stream().map(InteractionAction::toString).filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return config.saveToString();
    }

    public boolean spawnNpc(String yaml, Location pos) throws InvalidConfigurationException {
        YamlConfiguration npcConfig = new YamlConfiguration();
        npcConfig.loadFromString(yaml);

        String name = UUID.randomUUID().toString(); // Create a unique name

        UUID creator = UUID.randomUUID(); // Random creator


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
