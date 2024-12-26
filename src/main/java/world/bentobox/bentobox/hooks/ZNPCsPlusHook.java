package world.bentobox.bentobox.hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.util.NpcLocation;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.util.Util;

/**
 * Provides copy and pasting of ZNPCS Plus in blueprints https://github.com/Pyrbu/ZNPCsPlus
 *
 * @author tastybento
 * @since 3.2.0
 */
public class ZNPCsPlusHook extends Hook {

    private static final String VERSION = "2.0.0-SNAPSHOT"; // Minimum version required

    public ZNPCsPlusHook() {
        super("ZNPCsPlus", Material.PLAYER_HEAD);
    }

    public String serializeNPC(NpcEntry entry, Vector origin) {
        String result = NpcApiProvider.get().getNpcSerializerRegistry().getSerializer(YamlConfiguration.class)
                .serialize(entry)
                .saveToString();
        return result;
    }

    public boolean spawnNpc(String yaml, Location pos) throws InvalidConfigurationException {
        YamlConfiguration yaml2 = new YamlConfiguration();
        yaml2.loadFromString(yaml);
        NpcEntry entry = NpcApiProvider.get().getNpcSerializerRegistry().getSerializer(YamlConfiguration.class)
                .deserialize(yaml2);
        NpcLocation loc = new NpcLocation(pos);
        entry.getNpc().setLocation(loc);
        NpcApiProvider.get().getNpcRegistry().register(entry);

        return true;
    }

    @Override
    public boolean hook() {
        boolean hooked = this.isPluginAvailable();
        // Check version
        String version = this.getPlugin().getDescription().getVersion();
        if (!Util.isVersionCompatible(version, VERSION)) {
            return false;
        }
        if (!hooked) {
            BentoBox.getInstance().logError("Could not hook into FancyNpcs");
        }
        return hooked;
    }

    @Override
    public String getFailureCause() {
        // The only failure is wrong version
        return "ZNPCsPlus version " + VERSION + " required or later. You are running "
                + this.getPlugin().getDescription().getVersion();
    }

    public Map<? extends Vector, ? extends List<BlueprintEntity>> getNpcsInArea(World world, List<Vector> vectorsToCopy,
            @Nullable Vector origin) {
        Map<Vector, List<BlueprintEntity>> bpEntities = new HashMap<>();

        for (NpcEntry npcEntry : NpcApiProvider.get().getNpcRegistry().getAll()) {
            NpcLocation npcLocation = npcEntry.getNpc().getLocation();
            Vector loc = new Vector(npcLocation.getBlockX(), npcLocation.getBlockY(), npcLocation.getBlockZ());
            if (npcEntry.getNpc().getWorld().equals(world) && vectorsToCopy.contains(loc)) {
                // Put the NPC into a BlueprintEntity - serialize it
                BlueprintEntity cit = new BlueprintEntity();
                cit.setNpc(this.serializeNPC(npcEntry, origin));
                // Retrieve or create the list of entities and add this one
                List<BlueprintEntity> entities = bpEntities.getOrDefault(loc, new ArrayList<>());
                entities.add(cit);
                // Create the position where this entity will be pasted relative to the location
                Vector origin2 = origin == null ? new Vector(0, 0, 0) : origin;
                int x = loc.getBlockX() - origin2.getBlockX();
                int y = loc.getBlockY() - origin2.getBlockY();
                int z = loc.getBlockZ() - origin2.getBlockZ();
                Vector pos = new Vector(x, y, z);
                // Store
                bpEntities.put(pos, entities); // Update the map
            }
        }
        return bpEntities;
    }
}
