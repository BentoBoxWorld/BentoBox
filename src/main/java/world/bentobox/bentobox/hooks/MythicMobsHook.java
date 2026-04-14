package world.bentobox.bentobox.hooks;

import java.util.function.Consumer;

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
        return spawnMythicMob(mmr, spawnLocation, null);
    }

    /**
     * Spawn a MythicMob and run a callback once the entity has actually been spawned.
     * <p>
     * Delegates to {@link #spawnMythicMob(MythicMobRecord, Location, Consumer, long)}
     * with a 40-tick delay — the historical behaviour, required by blueprint-paste
     * callers so blocks settle before mobs land on them.
     *
     * @param mmr MythicMobRecord
     * @param spawnLocation location
     * @param onSpawn callback invoked with the spawned Bukkit entity; may be {@code null}
     * @return true if the mob type exists and a spawn was scheduled
     * @since 3.15.0
     */
    public boolean spawnMythicMob(MythicMobRecord mmr, Location spawnLocation, Consumer<Entity> onSpawn) {
        return spawnMythicMob(mmr, spawnLocation, onSpawn, 40L);
    }

    /**
     * Spawn a MythicMob with an explicit scheduler delay.
     * <p>
     * Blueprint-paste callers need a short delay so NMS-pasted blocks settle
     * before mobs land on them; synchronous callers (e.g. AOneBlock's
     * {@code MythicMobCustomBlock}) can pass {@code 0} to spawn immediately on
     * the current tick. When {@code delayTicks <= 0} the spawn runs inline and
     * the {@code onSpawn} callback is invoked synchronously.
     *
     * @param mmr MythicMobRecord
     * @param spawnLocation location
     * @param onSpawn callback invoked with the spawned Bukkit entity; may be {@code null}
     * @param delayTicks ticks to wait before spawning; {@code <= 0} = spawn immediately
     * @return true if the mob type exists and a spawn was scheduled (or ran)
     * @since 3.15.0
     */
    public boolean spawnMythicMob(MythicMobRecord mmr, Location spawnLocation,
            Consumer<Entity> onSpawn, long delayTicks) {
        if (!this.isPluginAvailable()) {
            return false;
        }
        return MythicBukkit.inst().getMobManager().getMythicMob(mmr.type()).map(mob -> {
            Runnable spawn = () -> {
                ActiveMob activeMob = mob.spawn(BukkitAdapter.adapt(spawnLocation), mmr.level());
                activeMob.setDisplayName(mmr.displayName());
                activeMob.setPower(mmr.power());
                activeMob.setStance(mmr.stance());
                if (onSpawn != null) {
                    Entity bukkitEntity = activeMob.getEntity().getBukkitEntity();
                    if (bukkitEntity != null) {
                        onSpawn.accept(bukkitEntity);
                    }
                }
            };
            if (delayTicks <= 0L) {
                spawn.run();
            } else {
                Bukkit.getScheduler().runTaskLater(getPlugin(), spawn, delayTicks);
            }
            return true;
        }).orElse(false);
    }
}
