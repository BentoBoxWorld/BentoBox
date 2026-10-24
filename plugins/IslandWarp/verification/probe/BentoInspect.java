package verification;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/** Read-only: logs every BentoBox island so ownership can be verified from the log. */
public final class BentoInspect extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getGlobalRegionScheduler().runDelayed(this, t -> {
            var manager = BentoBox.getInstance().getIslandsManager();
            getLogger().info("INSPECT_BEGIN");
            for (Island island : manager.getIslands()) {
                UUID owner = island.getOwner();
                String name = owner == null ? "null" : String.valueOf(
                        Bukkit.getOfflinePlayer(owner).getName());
                getLogger().info("INSPECT_ISLAND id=" + island.getUniqueId()
                        + " owner=" + owner
                        + " ownerName=" + name
                        + " deleted=" + island.isDeleted()
                        + " deletable=" + island.isDeletable()
                        + " center=" + island.getCenter()
                        + " members=" + island.getMemberSet().size());
            }
            getLogger().info("INSPECT_TOTAL=" + manager.getIslands().size());
            getLogger().info("INSPECT_END");
        }, 40);
    }
}
