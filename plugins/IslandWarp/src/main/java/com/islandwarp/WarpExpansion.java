package com.islandwarp;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

final class WarpExpansion extends PlaceholderExpansion {
    private final IslandWarpPlugin plugin;
    WarpExpansion(IslandWarpPlugin plugin) { this.plugin=plugin; }
    @Override public String getIdentifier() { return "islandwarp"; }
    @Override public String getAuthor() { return "XingXing"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player,String key) {
        return switch(key.toLowerCase(java.util.Locale.ROOT)) {
            case "total" -> Long.toString(plugin.store.all().values().stream().filter(plugin.store::publicAvailable).count());
            case "database_status" -> plugin.store.healthy()?"正常":"异常";
            case "num" -> player==null?"0":Long.toString(plugin.store.owned(player.getUniqueId()));
            case "favorites" -> player==null?"0":Long.toString(plugin.store.favorites(player.getUniqueId()).stream().filter(id -> { Warp w=plugin.store.all().get(id); return w!=null&&(plugin.store.publicAvailable(w)||w.owner().equals(player.getUniqueId())); }).count());
            case "visits" -> player==null?"0":Long.toString(plugin.store.all().values().stream().filter(w -> w.owner().equals(player.getUniqueId())).mapToLong(Warp::visits).sum());
            default -> null;
        };
    }
}
