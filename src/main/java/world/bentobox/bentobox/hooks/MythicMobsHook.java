package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

/**
 * Provides implementation and interfacing to interact with MythicMobs.
 *
 * @author tastybento
 * @since 2.2.0
 */
public class MythicMobsHook extends Hook {

    public MythicMobsHook() {
        super("MythicMobs", Material.CREEPER_HEAD);
    }

    public boolean isMythicMob(Entity bukkitEntity) {
        return MythicBukkit.inst().getMobManager().isMythicMob(bukkitEntity);
    }

    public MythicMobRecord getMythicMob(Entity bukkitEntity) {
        ActiveMob mm = MythicBukkit.inst().getMobManager().getActiveMob(bukkitEntity.getUniqueId()).orElse(null);
        if (mm != null) {
            return new MythicMobRecord(mm.getMobType(), mm.getDisplayName(), mm.getLevel(),
                    mm.getPower(),
                    mm.getStance());
        }
        return null;
    }


    @Override
    public boolean hook() {
        return true; // The hook process shouldn't fail
    }

    @Override
    public String getFailureCause() {
        return null; // The hook process shouldn't fail
    }

    /**
     * Spawn a MythicMob
     * @param mmr MythicMobRecord
     * @param spawnLocation location
     * @return true if spawn is successful
     */
    public boolean spawnMythicMob(MythicMobRecord mmr, Location spawnLocation) {
        return MythicBukkit.inst().getMobManager().getMythicMob(mmr.type()).map(mob -> {
            // A delay is required before spawning, I assume because the blocks are pasted using NMS
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                // spawns mob            
                ActiveMob activeMob = mob.spawn(BukkitAdapter.adapt(spawnLocation), mmr.level());
                activeMob.setDisplayName(mmr.displayName());
                activeMob.setPower(mmr.power());
                activeMob.setStance(mmr.stance());
            }, 40L);
            return true;
        }).orElse(false);
    }
}
