package com.islandwarp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;
import java.util.UUID;

final class IslandGuard {
    private final IslandWarpPlugin plugin;
    IslandGuard(IslandWarpPlugin plugin) { this.plugin = plugin; }

    private BentoBox islands() { return BentoBox.getInstance(); }

    String creation(Player p, Location location) {
        plugin.store.requireCacheReady();
        if (plugin.getConfig().getStringList("creation.blocked-worlds").contains(location.getWorld().getName()))
            throw new IllegalArgumentException("这个世界禁止创建地标");
        BentoBox box = islands();
        if (box == null) {
            if (plugin.getConfig().getBoolean("creation.own-island-only", true) && !p.hasPermission("islandwarp.admin"))
                throw new IllegalArgumentException("岛屿系统未连接，暂时无法校验岛屿");
            return null;
        }
        Optional<Island> here = box.getIslandsManager().getIslandAt(location).filter(i -> !i.isDeleted() && i.onIsland(location));
        if (plugin.getConfig().getBoolean("creation.own-island-only", true) && !p.hasPermission("islandwarp.admin")) {
            UUID player = p.getUniqueId();
            // BentoBox 的成员表含岛主本人（OWNER_RANK），所以成员判定已覆盖岛主。
            if (here.filter(i -> i.getMemberSet().contains(player)).isEmpty())
                throw new IllegalArgumentException("只能在自己拥有的岛屿内创建地标");
        }
        return here.map(Island::getUniqueId).orElse(null);
    }

    void access(Player player, Warp warp) {
        plugin.store.requireUnfrozen(warp.id());
        plugin.store.requireEventActive(warp.id());
        if(plugin.store.blocked(warp.id(),player.getUniqueId())) throw new IllegalArgumentException("坐标主人已禁止你传送到此坐标");
        if (warp.island() == null) return;
        BentoBox box = islands();
        if (box == null) throw new IllegalArgumentException("岛屿系统未连接，暂时不能访问这个地标");
        World world = plugin.getServer().getWorld(warp.world());
        if (world == null) throw new IllegalArgumentException("这个地标已不在原岛屿范围内");
        Location location = new Location(world, warp.x(), warp.y(), warp.z());
        // 位置当前所属的岛必须还是创建时那个，岛被删除或同坐标重建过都算失效。
        Island island = box.getIslandsManager().getIslandAt(location)
                .filter(i -> !i.isDeleted() && warp.island().equals(i.getUniqueId()) && i.onIsland(location))
                .orElseThrow(() -> new IllegalArgumentException("这个地标已不在原岛屿范围内"));
        // BentoBox 没有“岛屿私有/不开放访客”开关，访客限制靠岛屿等级与保护标志，这里只校验封禁。
        if (island.isBanned(player.getUniqueId()))
            throw new IllegalArgumentException("你被该岛屿禁止访问");
    }
}
